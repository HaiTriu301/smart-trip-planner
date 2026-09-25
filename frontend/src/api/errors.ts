import axios from 'axios'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ErrorResponse } from '../types/api'

const NETWORK_ERROR_MESSAGE = 'Không kết nối được máy chủ, vui lòng thử lại'

/** The backend ErrorResponse body, or undefined for network errors / non-API failures. */
export function getApiError(error: unknown): ErrorResponse | undefined {
  if (!axios.isAxiosError<ErrorResponse>(error)) return undefined
  const body = error.response?.data
  return body && body.success === false ? body : undefined
}

/** Vietnamese message from the backend (messages.properties), or a generic fallback. */
export function getErrorMessage(error: unknown): string {
  return getApiError(error)?.message ?? NETWORK_ERROR_MESSAGE
}

/**
 * Copies VALIDATION_ERROR details onto matching form fields. Returns true when at least one field was set,
 * so callers can skip the generic banner.
 */
export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): boolean {
  const details = getApiError(error)?.details ?? []
  let applied = false
  for (const detail of details) {
    const field = fields.find((name) => name === detail.field)
    if (field && detail.message) {
      setError(field, { type: 'server', message: detail.message })
      applied = true
    }
  }
  return applied
}
