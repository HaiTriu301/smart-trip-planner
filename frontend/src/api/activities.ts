import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Activity, CreateActivityRequest, UpdateActivityRequest } from '../types/activity'

// allowOverlap=false: a time range overlapping another activity of the day is refused with
// 409 ACTIVITY_TIME_CONFLICT; true saves it anyway after the user confirmed (design.md rule 14.4).

export async function createActivity(
  tripId: number,
  dayId: number,
  body: CreateActivityRequest,
  allowOverlap = false,
): Promise<Activity> {
  const { data } = await apiClient.post<ApiResponse<Activity>>(`/trips/${tripId}/days/${dayId}/activities`, body, {
    params: { allowOverlap },
  })
  return data.data
}

export async function updateActivity(
  tripId: number,
  activityId: number,
  body: UpdateActivityRequest,
  allowOverlap = false,
): Promise<Activity> {
  const { data } = await apiClient.patch<ApiResponse<Activity>>(`/trips/${tripId}/activities/${activityId}`, body, {
    params: { allowOverlap },
  })
  return data.data
}

/** Hard delete (design.md 10.2). */
export async function deleteActivity(tripId: number, activityId: number): Promise<void> {
  await apiClient.delete<ApiResponse<null>>(`/trips/${tripId}/activities/${activityId}`)
}
