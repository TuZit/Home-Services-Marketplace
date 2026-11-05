import { useQuery } from '@tanstack/react-query'
import { apiClient } from './client'
import type {
  Address,
  Booking,
  CustomerProfile,
  DashboardStats,
  EligibleProvidersResponse,
  GeoArea,
  ProviderProfile,
  Quotation,
  Review,
  Service,
  ServiceCategory,
  ServiceRequest,
  User,
} from '../types'

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: async () => (await apiClient.get<ServiceCategory[]>('/categories')).data,
  })
}

export function useCategoryServices(categoryId: string | undefined) {
  return useQuery({
    queryKey: ['category-services', categoryId],
    queryFn: async () => (await apiClient.get<Service[]>(`/categories/${categoryId}/services`)).data,
    enabled: Boolean(categoryId),
  })
}

export function useService(serviceId: string | undefined) {
  return useQuery({
    queryKey: ['service', serviceId],
    queryFn: async () => (await apiClient.get<Service>(`/services/${serviceId}`)).data,
    enabled: Boolean(serviceId),
  })
}

export function useAreas() {
  return useQuery({
    queryKey: ['areas'],
    queryFn: async () => (await apiClient.get<GeoArea[]>('/areas')).data,
    staleTime: 60 * 60 * 1000,
  })
}

export function useAddresses() {
  return useQuery({
    queryKey: ['addresses'],
    queryFn: async () => (await apiClient.get<Address[]>('/me/addresses')).data,
  })
}

/** The auth session user carries no display name, so customers read it from their profile. */
export function useCustomerProfile(enabled: boolean) {
  return useQuery({
    queryKey: ['customer-profile'],
    queryFn: async () => (await apiClient.get<CustomerProfile>('/me')).data,
    enabled,
    staleTime: 5 * 60 * 1000,
  })
}

export function useServiceRequest(id: string | undefined) {
  return useQuery({
    queryKey: ['service-request', id],
    queryFn: async () => (await apiClient.get<ServiceRequest>(`/service-requests/${id}`)).data,
    enabled: Boolean(id),
  })
}

/** Discovery only runs while the request is CONFIRMED; the backend rejects every other state. */
export function useEligibleProviders(requestId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: ['eligible-providers', requestId],
    queryFn: async () =>
      (await apiClient.get<EligibleProvidersResponse>(`/service-requests/${requestId}/eligible-providers`))
        .data,
    enabled: Boolean(requestId) && enabled,
  })
}

export function useQuotation(id: string | undefined) {
  return useQuery({
    queryKey: ['quotation', id],
    queryFn: async () => (await apiClient.get<Quotation>(`/quotations/${id}`)).data,
    enabled: Boolean(id),
  })
}

export function useBooking(id: string | undefined) {
  return useQuery({
    queryKey: ['booking', id],
    queryFn: async () => (await apiClient.get<Booking>(`/bookings/${id}`)).data,
    enabled: Boolean(id),
  })
}

export function useProviderProfile() {
  return useQuery({
    queryKey: ['provider-profile'],
    queryFn: async () => (await apiClient.get<ProviderProfile>('/provider/profile')).data,
  })
}

export function useAdminUsers() {
  return useQuery({
    queryKey: ['admin-users'],
    queryFn: async () => (await apiClient.get<User[]>('/admin/users')).data,
  })
}

export function useAdminDashboard() {
  return useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: async () => (await apiClient.get<DashboardStats>('/admin/dashboard')).data,
  })
}

export function useAdminReviews() {
  return useQuery({
    queryKey: ['admin-reviews'],
    queryFn: async () => (await apiClient.get<Review[]>('/admin/reviews')).data,
  })
}
