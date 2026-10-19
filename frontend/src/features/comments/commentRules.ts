import type { Comment } from '../../types/comment'
import type { TripRole } from '../../types/member'

/**
 * Who sees the delete button of a comment (design.md 6.2, rule 27): its author, whatever their role, and the owner
 * of the trip, for every comment. An editor or a viewer cannot delete what others wrote. The server checks the
 * same rule; this only spares people a button that would end in 403.
 *
 * @param userId the signed-in account, null while it is not known
 */
export function canDeleteComment(comment: Comment, userId: number | null, myRole: TripRole): boolean {
  return myRole === 'OWNER' || (userId !== null && comment.author.id === userId)
}

/**
 * The comments that disappear with a deletion: the comment, and its replies when it is a root (the server deletes
 * them in the same go). What the list drops at once, before it is reloaded.
 */
export function withoutDeleted(comments: Comment[], deletedId: number): Comment[] {
  return comments.filter((comment) => comment.id !== deletedId && comment.parentId !== deletedId)
}
