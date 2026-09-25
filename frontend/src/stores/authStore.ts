import { create } from 'zustand'
import type { AuthResponse, UserResponse } from '../types/auth'

/**
 * unknown       – app just loaded, the initial /auth/refresh has not answered yet
 * authenticated – access token in memory
 * anonymous     – no valid session (never logged in, logged out, or refresh rejected)
 */
export type AuthStatus = 'unknown' | 'authenticated' | 'anonymous'

interface AuthState {
  status: AuthStatus
  // Access token lives only in memory (design.md 6.1); the refresh token is an httpOnly cookie
  accessToken: string | null
  user: UserResponse | null
  setSession: (auth: AuthResponse) => void
  clearSession: () => void
}

export const useAuthStore = create<AuthState>()((set) => ({
  status: 'unknown',
  accessToken: null,
  user: null,
  setSession: (auth) =>
    set({ status: 'authenticated', accessToken: auth.accessToken, user: auth.user }),
  clearSession: () => set({ status: 'anonymous', accessToken: null, user: null }),
}))
