import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Comment } from '../types/comment'

/**
 * Cache key of the comments of a trip: the trip page, the header button and the panel all read the same list, so
 * it is asked for once (design.md 10.2: the counts on the screen come from this list, not from ActivityResponse).
 */
export const commentsKey = (tripId: number) => ['comments', tripId]

/**
 * Every comment of the trip, roots and replies in one flat list, oldest first (createdAt, then id). No paging
 * (design.md 10.2). Anybody who can see the trip can read them; 404 when the trip is gone.
 */
export async function listComments(tripId: number): Promise<Comment[]> {
  const { data } = await apiClient.get<ApiResponse<Comment[]>>(`/trips/${tripId}/comments`)
  return data.data
}
