import { apiClient } from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { TripListParams, TripSummary } from '../types/trip'

/** Trips of the signed-in user, newest first (backend default sort=createdAt,desc). */
export async function listTrips(params: TripListParams): Promise<PageResponse<TripSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<TripSummary>>>('/trips', { params })
  return data.data
}
