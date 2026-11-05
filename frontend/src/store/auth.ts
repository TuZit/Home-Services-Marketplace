import { create } from 'zustand'
import { apiClient, refreshAccessToken } from '../api/client'
import type { AuthResponse, Role, User } from '../types'

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput {
  fullName: string
  email: string
  password: string
  role: Role
  phone?: string
}

interface AuthState {
  accessToken: string | null
  user: User | null
  sessionReady: boolean
  login: (input: LoginInput) => Promise<User>
  register: (input: RegisterInput) => Promise<void>
  logout: () => Promise<void>
  setSession: (session: AuthResponse) => void
  clearSession: () => void
  restoreSession: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  sessionReady: false,

  login: async (input) => {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', input)
    set({ accessToken: data.accessToken, user: data.user, sessionReady: true })
    return data.user
  },

  register: async (input) => {
    await apiClient.post('/auth/register', input)
  },

  logout: async () => {
    try {
      await apiClient.post('/auth/logout')
    } catch {
      // Session is invalidated locally even if the server call fails.
    } finally {
      set({ accessToken: null, user: null, sessionReady: true })
    }
  },

  setSession: (session) => {
    set({ accessToken: session.accessToken, user: session.user, sessionReady: true })
  },

  clearSession: () => {
    set({ accessToken: null, user: null, sessionReady: true })
  },

  restoreSession: async () => {
    try {
      const session = await refreshAccessToken()
      set({ accessToken: session.accessToken, user: session.user, sessionReady: true })
    } catch {
      set({ accessToken: null, user: null, sessionReady: true })
    }
  },
}))
