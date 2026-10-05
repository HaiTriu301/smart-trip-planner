import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { getErrorMessage, isRejectedByServer, isWorthRetrying } from './errors'

const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig

/** An error as axios throws it when the server answered with this status and this body. */
function answered(status: number, data: unknown): AxiosError {
  return new AxiosError(`Request failed with status code ${status}`, AxiosError.ERR_BAD_RESPONSE, config, {}, {
    status,
    statusText: '',
    data,
    headers: {},
    config,
  })
}

/** The body the backend sends with every error (ErrorResponse). */
function apiError(errorCode: string, message = 'Thông báo từ máy chủ') {
  return { success: false, errorCode, message, details: [], timestamp: '2026-10-05T10:00:00Z', path: '/api/v1/x' }
}

const networkDown = new AxiosError('Network Error', AxiosError.ERR_NETWORK, config, {})
const timedOut = new AxiosError('timeout of 10000ms exceeded', AxiosError.ECONNABORTED, config, {})

describe('isWorthRetrying', () => {
  it('retries what may pass on its own: no network, a timeout, a failure of the server', () => {
    expect(isWorthRetrying(networkDown)).toBe(true)
    expect(isWorthRetrying(timedOut)).toBe(true)
    expect(isWorthRetrying(answered(500, apiError('INTERNAL_ERROR')))).toBe(true)
    // A proxy in front of the backend answers with a page, not with an ErrorResponse
    expect(isWorthRetrying(answered(502, '<html>Bad Gateway</html>'))).toBe(true)
    expect(isWorthRetrying(answered(503, '<html>Service Unavailable</html>'))).toBe(true)
  })

  it('does not retry when the backend says an outside service gave no answer', () => {
    // The backend already tried the service up to three times, or stopped calling it for a while
    const error = answered(503, apiError('PROVIDER_UNAVAILABLE', 'Dịch vụ bên ngoài tạm thời không khả dụng'))

    expect(isWorthRetrying(error)).toBe(false)
    // The message of the backend is still what the user reads
    expect(getErrorMessage(error)).toBe('Dịch vụ bên ngoài tạm thời không khả dụng')
  })

  it('does not retry a request the server refused', () => {
    expect(isWorthRetrying(answered(400, apiError('VALIDATION_ERROR')))).toBe(false)
    expect(isWorthRetrying(answered(403, apiError('FORBIDDEN')))).toBe(false)
    expect(isWorthRetrying(answered(404, apiError('RESOURCE_NOT_FOUND')))).toBe(false)
    expect(isWorthRetrying(answered(429, apiError('RATE_LIMITED')))).toBe(false)
  })

  it('goes by the error code, not by the status: another 503 is retried', () => {
    expect(isWorthRetrying(answered(503, apiError('INTERNAL_ERROR')))).toBe(true)
  })

  it('retries a failure that did not come from a request at all', () => {
    // Same answer as before this rule existed: only what the server said is final
    expect(isWorthRetrying(new Error('boom'))).toBe(true)
  })
})

describe('isRejectedByServer', () => {
  it('is true for every 4xx and only for them', () => {
    expect(isRejectedByServer(answered(400, apiError('VALIDATION_ERROR')))).toBe(true)
    expect(isRejectedByServer(answered(499, {}))).toBe(true)
    expect(isRejectedByServer(answered(500, apiError('INTERNAL_ERROR')))).toBe(false)
    expect(isRejectedByServer(answered(503, apiError('PROVIDER_UNAVAILABLE')))).toBe(false)
    expect(isRejectedByServer(networkDown)).toBe(false)
    expect(isRejectedByServer(new Error('boom'))).toBe(false)
  })
})
