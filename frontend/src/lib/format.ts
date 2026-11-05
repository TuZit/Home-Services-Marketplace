import type { Money } from '../types'

export function formatMoney(money?: Money | null): string {
  if (!money) {
    return '—'
  }
  const { amount, currency } = money
  try {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currency || 'USD',
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
