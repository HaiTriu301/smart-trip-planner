import { apiClient } from './client'
import type { ApiResponse } from '../types/api'

export async function ping(): Promise<ApiResponse<string>> {
  const response = await apiClient.get<ApiResponse<string>>('/ping')
  return response.data
}