import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { TripWeather } from '../types/weather'

/** The forecast of every day of a trip, taken at the trip's destination (design.md rule 14.20). */
export async function getTripWeather(tripId: number): Promise<TripWeather> {
  const { data } = await apiClient.get<ApiResponse<TripWeather>>(`/weather/trips/${tripId}`)
  return data.data
}
