import { isAxiosError } from 'axios'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ApiErrorBody } from '../types'

export function getErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (isAxiosError<ApiErrorBody>(error)) {
    const data = error.response?.data

    if (data?.code === 'SLOT_UNAVAILABLE') {
      return 'That time slot is no longer available. Please choose a different slot and try again.'
    }

    if (data?.message) {
      return data.message
    }

    switch (error.response?.status) {
      case 401:
        return 'Your session has expired. Please log in again.'
      case 403:
        return 'You do not have permission to perform this action.'
      case 404:
        return 'The requested resource was not found.'
      case 409:
        return 'This action conflicts with the current state. Please refresh and try again.'
      case 422:
        return 'Please review the highlighted fields and try again.'
      default:
        return fallback
    }
  }

  return fallback
}

export function getFieldErrors(error: unknown): Record<string, string> {
  if (!isAxiosError<ApiErrorBody>(error)) {
    return {}
  }

  const errors = error.response?.data?.errors
  if (!errors) {
    return {}
  }

  if (Array.isArray(errors)) {
    return {}
  }

  const result: Record<string, string> = {}
  for (const [key, value] of Object.entries(errors)) {
    result[key] = Array.isArray(value) ? value.join(', ') : String(value)
  }
  return result
}

export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): void {
  const fieldErrors = getFieldErrors(error)
  for (const [field, message] of Object.entries(fieldErrors)) {
    setError(field as unknown as Path<T>, { type: 'server', message })
  }
}
