// Types shaped from the backend API contracts. The customer-facing flow mirrors the
// request/discovery DTOs exactly; the remaining shapes are pragmatic where an endpoint
// has not been wired up yet.

export type Role = 'CUSTOMER' | 'PROVIDER' | 'ADMIN'

export type UserStatus = 'ACTIVE' | 'SUSPENDED'

export interface User {
  id: string
  email: string
  role: Role
  status?: UserStatus
  /** Only present on the customer profile endpoint, not on the auth session user. */
  fullName?: string | null
}

export interface AuthResponse {
  accessToken: string
  user: User
}

export interface Money {
  amount: number
  currency: string
}

// ------------------------------------------------------------------ catalogue

export interface ServiceCategory {
  id: string
  name: string
  slug: string
  description?: string | null
  active: boolean
}

export interface Service {
  id: string
  categoryId: string
  name: string
  slug: string
  description?: string | null
  active: boolean
}

export interface GeoArea {
  code: string
  name: string
  type: 'province' | 'district'
  parentCode?: string | null
}

// --------------------------------------------------------------- address book

export interface Address {
  id: string
  label?: string | null
  addressLine: string
  areaCode?: string | null
  isDefault: boolean
}

export interface AddressInput {
  label: string
  addressLine: string
  areaCode: string
  isDefault?: boolean
}

export interface CustomerProfile {
  id: string
  fullName?: string | null
  phone?: string | null
}

// ------------------------------------------------------------ service requests

export type RequestStatus =
  | 'DRAFT'
  | 'CONFIRMED'
  | 'PROVIDERS_FOUND'
  | 'PROVIDER_SELECTED'
  | 'CANCELLED'

/** Immutable content snapshot attached to a request. */
export interface RequestDetails {
  version: number
  categoryId?: string | null
  serviceId?: string | null
  description?: string | null
  addressSnapshot?: string | null
  areaCode?: string | null
  preferredStart?: string | null
  preferredEnd?: string | null
  budgetAmount?: number | null
  budgetCurrency?: string | null
}

export interface ServiceRequest {
  id: string
  status: RequestStatus
  currentVersion: number
  details: RequestDetails
  createdAt?: string
  updatedAt?: string
}

export interface ServiceRequestInput {
  categoryId: string
  serviceId: string
  description: string
  addressId: string
  preferredStart: string
  preferredEnd: string
  budgetAmount?: number | null
  budgetCurrency?: string | null
}

// ------------------------------------------------------------------ discovery

export interface AvailabilityWindow {
  /** 0 = Sunday .. 6 = Saturday */
  dayOfWeek: number
  startTime: string
  endTime: string
}

export interface EligibleProvider {
  providerId: string
  businessName?: string | null
  description?: string | null
  experienceSummary?: string | null
  reasonCodes?: string[]
  availabilityWindows?: AvailabilityWindow[]
}

export interface EligibleProvidersResponse {
  requestId?: string
  requestVersion?: number
  discoveryRunId?: string
  noResultReason?: string | null
  providers?: EligibleProvider[]
}

export interface ProviderSelection {
  id: string
  requestId: string
  providerId: string
  requestVersion: number
  status: 'ACTIVE' | 'CONVERTED' | 'RELEASED'
  selectedAt?: string
}

// ------------------------------------------- other roles (not yet re-aligned)

export interface Quotation {
  id: string
  requestId: string
  providerId: string
  providerName?: string
  status: string
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
  status: string
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
