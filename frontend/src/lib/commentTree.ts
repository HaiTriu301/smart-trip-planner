import type { Comment } from '../types/comment'

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
