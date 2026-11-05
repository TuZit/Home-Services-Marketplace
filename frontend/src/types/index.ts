// Shared DTO types matching the backend API contracts (pragmatic shapes where
// the contract does not spell out every field).

export type Role = 'CUSTOMER' | 'PROVIDER' | 'ADMIN'

export type RequestStatus =
  | 'DRAFT'
  | 'CONFIRMED'
  | 'PENDING'
  | 'PROVIDER_SELECTED'
  | 'CANCELLED'
  | 'COMPLETED'

export type QuotationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'WITHDRAWN'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'EXPIRED'

export type BookingStatus =
  | 'RESERVED'
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'

export interface Money {
  amount: number
  currency: string
}

export interface User {
  id: string
  email: string
  fullName: string
  role: Role
  phone?: string | null
  status?: string
  verified?: boolean
  createdAt?: string
}

export interface AuthResponse {
  accessToken: string
  user: User
}

export interface ServiceCategory {
  id: string
  name: string
  slug?: string
  description?: string | null
  iconUrl?: string | null
  active: boolean
  serviceCount?: number
}

export interface Service {
  id: string
  categoryId: string
  name: string
  description?: string | null
  price?: Money
  basePrice?: Money
  durationMinutes?: number | null
  active: boolean
}

export interface Address {
  id: string
  line1: string
  line2?: string | null
  city: string
  state?: string | null
  postalCode?: string | null
  country: string
  latitude?: number | null
  longitude?: number | null
  label?: string | null
}

export interface ServiceRequest {
  id: string
  customerId?: string
  categoryId?: string
  serviceId: string
  service?: Service
  status: RequestStatus | string
  title: string
  description: string
  address?: Address
  addressId?: string
  preferredDate?: string | null
  preferredTime?: string | null
  budget?: Money
  version?: number
  confirmedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface EligibleProvider {
  providerId: string
  fullName: string
  businessName?: string | null
  rating?: number
  reviewCount?: number
  distanceKm?: number | null
  basePrice?: Money
  estimatedPrice?: Money
  available: boolean
  nextAvailableSlot?: string | null
  services?: Service[]
  verified?: boolean
}

export interface Quotation {
  id: string
  requestId: string
  providerId: string
  providerName?: string
  status: QuotationStatus | string
  amount: Money
  description?: string | null
  validUntil?: string | null
  createdAt?: string
  submittedAt?: string | null
}

export interface Booking {
  id: string
  quotationId?: string
  requestId?: string
  customerId?: string
  providerId?: string
  providerName?: string
  status: BookingStatus | string
  scheduledAt?: string | null
  startedAt?: string | null
  completedAt?: string | null
  cancelledAt?: string | null
  total?: Money
  createdAt?: string
}

export interface Review {
  id: string
  bookingId: string
  customerId?: string
  providerId?: string
  rating: number
  comment?: string | null
  hidden: boolean
  createdAt?: string
}

export interface Payment {
  id: string
  bookingId: string
  amount: Money
  status: string
  createdAt?: string
}

export interface ProviderProfile {
  providerId: string
  fullName: string
  businessName?: string | null
  bio?: string | null
  phone?: string | null
  email?: string
  verified: boolean
  rating?: number
  reviewCount?: number
  services?: Service[]
  areas?: string[]
  availability?: Record<string, unknown>
}

export interface DashboardStats {
  totalUsers?: number
  totalProviders?: number
  totalRequests?: number
  totalBookings?: number
  pendingVerifications?: number
  revenue?: Money
  recentBookings?: Booking[]
  [key: string]: unknown
}

export interface ApiErrorBody {
  code?: string
  message?: string
  errors?: Record<string, string[] | string> | string[]
}
