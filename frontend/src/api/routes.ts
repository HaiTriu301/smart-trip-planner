import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { DayRoute } from '../types/route'

/** Travel between the activities of one day that have a place, in the order the day shows them. */
export async function getDayRoute(tripId: number, dayId: number): Promise<DayRoute> {
  const { data } = await apiClient.get<ApiResponse<DayRoute>>(`/trips/${tripId}/days/${dayId}/route`)
  return data.data
}
