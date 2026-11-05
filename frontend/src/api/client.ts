import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../store/auth'
import type { AuthResponse } from '../types'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined

export const BASE_URL = configuredBaseUrl ?? 'http://localhost:8080/api/v1'

export const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
})

// Attach the in-memory access token and an idempotency key for writes.
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  const method = (config.method ?? 'get').toLowerCase()
  if (['post', 'put', 'patch', 'delete'].includes(method)) {
    if (!config.headers['Idempotency-Key']) {
      config.headers['Idempotency-Key'] =
        typeof crypto !== 'undefined' && 'randomUUID' in crypto
          ? crypto.randomUUID()
          : `${Date.now()}-${Math.random().toString(36).slice(2)}`
    }
  }

  return config
})

// Shared in-flight refresh promise so concurrent 401s trigger a single refresh.
let refreshPromise: Promise<AuthResponse> | null = null

export async function refreshAccessToken(): Promise<AuthResponse> {
  if (!refreshPromise) {
    refreshPromise = axios
      .post<AuthResponse>(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true })
      .then((response) => response.data)
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

// On 401, try to rotate the refresh cookie once and replay the original request.
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined

    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true
      try {
        const session = await refreshAccessToken()
        useAuthStore.getState().setSession(session)
        original.headers.Authorization = `Bearer ${session.accessToken}`
        return apiClient(original)
      } catch (refreshError) {
        useAuthStore.getState().clearSession()
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  },
)
