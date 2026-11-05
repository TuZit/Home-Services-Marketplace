import type { BadgeTone } from '../components/ui/Badge'
import type { RequestStatus } from '../types'

export interface StatusMeta {
  label: string
  tone: BadgeTone
  description: string
}

/** The happy path a service request travels through. */
export const REQUEST_FLOW: RequestStatus[] = [
  'DRAFT',
  'CONFIRMED',
  'PROVIDERS_FOUND',
  'PROVIDER_SELECTED',
]

export const REQUEST_STEPS = [
  { key: 'DRAFT', label: 'Draft' },
  { key: 'CONFIRMED', label: 'Confirmed' },
  { key: 'PROVIDERS_FOUND', label: 'Providers found' },
  { key: 'PROVIDER_SELECTED', label: 'Provider selected' },
]

const REQUEST_STATUS: Record<RequestStatus, StatusMeta> = {
  DRAFT: {
    label: 'Draft',
    tone: 'neutral',
    description: 'Only you can see this request. Confirm it to open it up to providers.',
  },
  CONFIRMED: {
    label: 'Confirmed',
    tone: 'brand',
    description: 'We are searching for providers who cover this service and area.',
  },
  PROVIDERS_FOUND: {
    label: 'Providers found',
    tone: 'accent',
    description: 'Providers matched your request. Compare them and pick the one you want.',
  },
  PROVIDER_SELECTED: {
    label: 'Provider selected',
    tone: 'success',
    description: 'You have chosen a provider. They will follow up with a quote.',
  },
  CANCELLED: {
    label: 'Cancelled',
    tone: 'danger',
    description: 'This request was cancelled and is no longer visible to providers.',
  },
}

const UNKNOWN: StatusMeta = {
  label: 'Unknown',
  tone: 'neutral',
  description: 'This request is in a state we do not recognise.',
}

export function requestStatusMeta(status?: string | null): StatusMeta {
  if (!status) {
    return UNKNOWN
  }
  return REQUEST_STATUS[status as RequestStatus] ?? { ...UNKNOWN, label: prettyCode(status) }
}

/** Position on the happy path. Cancelled requests stay at their furthest point. */
export function requestStepIndex(status?: string | null): number {
  if (!status) {
    return 0
  }
  const index = REQUEST_FLOW.indexOf(status as RequestStatus)
  return index === -1 ? 0 : index
}

/** `AREA_MATCH` → `Area match`. Kept generic so unknown codes still read cleanly. */
export function prettyCode(code: string): string {
  const words = code.replace(/[_-]+/g, ' ').trim().toLowerCase()
  if (!words) {
    return code
  }
  return words.charAt(0).toUpperCase() + words.slice(1)
}
