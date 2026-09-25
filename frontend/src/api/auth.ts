import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UserResponse,
} from '../types/auth'

export async function register(body: RegisterRequest): Promise<UserResponse> {
  const { data } = await apiClient.post<ApiResponse<UserResponse>>('/auth/register', body)
  return data.data
}

export async function login(body: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', body)
  return data.data
}

/** Needs a live access token; the client interceptor refreshes it first if it has expired. */
export async function logout(): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/logout')
}

export async function verifyEmail(token: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/verify-email', { token })
}

export async function resendVerification(email: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/resend-verification', { email })
}

export async function forgotPassword(email: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/forgot-password', { email })
}

export async function resetPassword(body: ResetPasswordRequest): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/reset-password', body)
}
