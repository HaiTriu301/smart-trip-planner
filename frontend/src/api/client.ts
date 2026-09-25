import axios, { type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse, ErrorResponse } from '../types/api'
import type { AuthResponse } from '../types/auth'
import { useAuthStore } from '../stores/authStore'

// Falls back to a relative path so requests always go through the dev proxy / nginx
const baseURL = import.meta.env.VITE_API_URL || '/api/v1'

export const apiClient = axios.create({
  baseURL,
  timeout: 10_000,
  // Refresh token travels in an httpOnly cookie (design.md 6.1)
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

const REFRESH_URL = '/auth/refresh'
const REFRESH_LOCK = 'trip-planner:auth-refresh'

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

let refreshInFlight: Promise<string> | null = null

/**
 * Rotates the refresh cookie and stores the new access token. Single-flight: concurrent callers share one
 * request, and a Web Lock serializes tabs. The backend treats a second use of an already-rotated cookie as
 * token theft and revokes every session, so two parallel refreshes must never happen.
 * On failure the session is cleared and the error is rethrown.
 */
export function refreshAccessToken(): Promise<string> {
  refreshInFlight ??= withCrossTabLock(requestNewToken).finally(() => {
    refreshInFlight = null
  })
  return refreshInFlight
}

function withCrossTabLock<T>(task: () => Promise<T>): Promise<T> {
  // Tabs share the cookie jar: once the lock holder rotates it, the next tab sends the new cookie
  if (!navigator.locks) return task()
  return navigator.locks.request(REFRESH_LOCK, task)
}

async function requestNewToken(): Promise<string> {
  try {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>(REFRESH_URL)
    useAuthStore.getState().setSession(data.data)
    return data.data.accessToken
  } catch (error) {
    useAuthStore.getState().clearSession()
    throw error
  }
}

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token && config.url !== REFRESH_URL) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError<ErrorResponse>(error) || !error.config || error.response?.status !== 401) {
      return Promise.reject(error)
    }
    const config: RetriableConfig = error.config
    if (config.url === REFRESH_URL) {
      return Promise.reject(error)
    }

    // Only TOKEN_EXPIRED is worth a refresh; UNAUTHORIZED / INVALID_CREDENTIALS are final (design.md 6.1)
    if (error.response.data?.errorCode === 'TOKEN_EXPIRED' && !config._retried) {
      config._retried = true
      const sentHeader = config.headers.Authorization
      const current = useAuthStore.getState().accessToken
      // Another request already refreshed while this one was in flight: just resend with the new token
      if (!current || sentHeader === `Bearer ${current}`) {
        await refreshAccessToken()
      }
      return apiClient(config)
    }

    // A token we sent was rejected: drop the session so ProtectedRoute sends the user to /login
    if (config.headers.Authorization) {
      useAuthStore.getState().clearSession()
    }
    return Promise.reject(error)
  },
)
