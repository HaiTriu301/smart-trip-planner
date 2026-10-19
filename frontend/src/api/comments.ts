import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Comment, CreateCommentRequest } from '../types/comment'

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

/** Names the post mutation, so the comment panel stays open until the server has answered (BUG-UI-004). */
export const createCommentKey = (tripId: number) => ['create-comment', tripId]

/**
 * Posts a comment (anybody who can see the trip, design.md 6.2) → 201 with the stored comment. 400 VALIDATION_ERROR
 * on field content when it is blank or longer than 2000 characters; 404 when the trip is gone.
 */
export async function createComment(tripId: number, body: CreateCommentRequest): Promise<Comment> {
  const { data } = await apiClient.post<ApiResponse<Comment>>(`/trips/${tripId}/comments`, body)
  return data.data
}

/**
 * Deletes a comment (soft delete): its author or the owner of the trip, otherwise 403 FORBIDDEN. Deleting a root
 * comment deletes its replies too. 404 when the comment is not on this trip or already deleted (design.md 10.2).
 */
export async function deleteComment(tripId: number, commentId: number): Promise<void> {
  await apiClient.delete(`/trips/${tripId}/comments/${commentId}`)
}
