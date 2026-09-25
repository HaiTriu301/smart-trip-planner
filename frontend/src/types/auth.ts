// Mirrors backend dto/request + dto/response for /api/v1/auth (design.md 6.1, 10.2).
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

export type Role = 'USER' | 'ADMIN'
export type Plan = 'FREE' | 'PREMIUM'
export type UserStatus = 'ACTIVE' | 'BLOCKED'

export interface UserResponse {
  id: number
  email: string
  fullName: string
  avatarUrl: string | null
  timezone: string
  locale: string
  role: Role
  plan: Plan
  planExpiresAt: string | null
  emailVerified: boolean
  status: UserStatus
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  /** Seconds until the access token expires */
  expiresIn: number
  user: UserResponse
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  confirmPassword: string
  fullName: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
  confirmPassword: string
}
