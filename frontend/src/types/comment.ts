// Mirrors backend dto/response/CommentResponse for /api/v1/trips/{tripId}/comments (design.md 10.2 "Quy ước Comment API").

/** Who wrote a comment: only what a fellow traveller may see, no email. */
export interface CommentAuthor {
  id: number
  fullName: string
  avatarUrl: string | null
}

/**
 * One comment of a trip. The list comes flat, oldest first (createdAt, then id); parentId lets the screen nest a
 * reply under its root, one level deep.
 */
export interface Comment {
  id: number
  /** The activity it is about; null for a comment on the whole trip. A reply carries the activity of its root */
  activityId: number | null
  /** The root comment it replies to; null for a root comment */
  parentId: number | null
  author: CommentAuthor
  /** Plain text, trimmed, line breaks kept (no HTML, no markdown) */
  content: string
  /** ISO Instant */
  createdAt: string
}

/**
 * Body of POST /trips/{tripId}/comments: content of 1–2000 characters, measured before the server trims it. The
 * author is the signed-in user, never sent (CLAUDE.md rule 16).
 */
export interface CreateCommentRequest {
  content: string
  /** The activity it is about; it must belong to the trip, otherwise 404. Ignored on a reply (design.md 10.2) */
  activityId?: number
  /**
   * The root comment answered. It must be a live root comment of the trip, otherwise 400 on this field; a reply
   * takes the activity of its root (design.md 10.2)
   */
  parentId?: number
}
