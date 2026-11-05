import type { Money } from '../types'

export function formatMoney(money?: Money | null): string {
  if (!money || typeof money.amount !== 'number') {
    return '—'
  }
  const { amount, currency } = money
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currency || 'VND',
      maximumFractionDigits: currency === 'VND' ? 0 : 2,
    }).format(amount)
  } catch {
    return `${amount} ${currency}`
  }
}

export function formatDate(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

/** Date plus time, e.g. "10 Nov 2026, 09:00". */
export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** A preferred window, e.g. "10 Nov, 09:00 → 11 Nov, 12:00". */
export function formatWindow(start?: string | null, end?: string | null): string {
  if (!start && !end) {
    return '—'
  }
  return `${formatDateTime(start)} → ${formatDateTime(end)}`
}

export const DAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

/** Trims a backend LocalTime (`09:00:00`) down to `09:00`. */
export function formatTimeOfDay(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const parts = value.split(':')
  return parts.length >= 2 ? `${parts[0]}:${parts[1]}` : value
}

export function statusLabel(status?: string | null): string {
  if (!status) {
    return 'Unknown'
  }
  return status
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
