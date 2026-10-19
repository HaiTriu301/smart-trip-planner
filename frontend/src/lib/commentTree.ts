import type { Activity } from '../types/activity'
import type { Comment } from '../types/comment'
import type { TripDayDetail } from '../types/trip'

/** A root comment with its replies, one level deep (design.md rule 27). */
export interface CommentThread {
  comment: Comment
  /** Oldest first, like the list of the API */
  replies: Comment[]
}

/**
 * Turns the flat list of GET /trips/{id}/comments into threads: every root comment in the order of the list, each
 * with the replies that point to it. A reply whose root is not in the list (it was deleted after the list was read,
 * or it points to another reply, which the API refuses) is left out rather than shown as a root: it would read as
 * a new conversation.
 */
export function commentThreads(comments: Comment[]): CommentThread[] {
  const threads = new Map<number, CommentThread>()
  for (const comment of comments) {
    if (comment.parentId === null) threads.set(comment.id, { comment, replies: [] })
  }
  for (const comment of comments) {
    if (comment.parentId !== null) threads.get(comment.parentId)?.replies.push(comment)
  }
  return Array.from(threads.values())
}

/**
 * Every activity of the trip by its id, whatever its day: what a comment's activityId names (its title and type for
 * the chip). An id missing from the map is an activity the trip as loaded does not have (deleted, or added after
 * the page loaded): the comment is shown without a chip rather than hidden.
 */
export function activitiesById(days: TripDayDetail[]): Map<number, Activity> {
  return new Map(days.flatMap((day) => day.activities.map((activity) => [activity.id, activity] as const)))
}
