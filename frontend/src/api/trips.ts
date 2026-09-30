import { apiClient } from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type {
  CreateTripRequest,
  TripDay,
  TripDetail,
  TripListParams,
  TripResponse,
  TripSummary,
  UpdateTripDayRequest,
} from '../types/trip'

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

/** Trip with days and activities; 404 when missing or deleted, 403 when not allowed (design.md 6.2). */
export async function getTrip(id: number): Promise<TripDetail> {
  const { data } = await apiClient.get<ApiResponse<TripDetail>>(`/trips/${id}`)
  return data.data
}

export async function updateTripDay(tripId: number, dayId: number, body: UpdateTripDayRequest): Promise<TripDay> {
  const { data } = await apiClient.patch<ApiResponse<TripDay>>(`/trips/${tripId}/days/${dayId}`, body)
  return data.data
}
