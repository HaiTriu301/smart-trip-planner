import { apiClient } from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { CreateTripRequest, TripListParams, TripResponse, TripSummary } from '../types/trip'

/** Trips of the signed-in user, newest first (backend default sort=createdAt,desc). */
export async function listTrips(params: TripListParams): Promise<PageResponse<TripSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<TripSummary>>>('/trips', { params })
  return data.data
}

/** The backend creates one TripDay per date of the range (design.md rule 14.2). */
export async function createTrip(body: CreateTripRequest): Promise<TripResponse> {
  const { data } = await apiClient.post<ApiResponse<TripResponse>>('/trips', body)
  return data.data
}
