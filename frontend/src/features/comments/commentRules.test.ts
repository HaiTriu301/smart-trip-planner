import { describe, expect, it } from 'vitest'
import type { Comment } from '../../types/comment'
import { canDeleteComment, withoutDeleted } from './commentRules'

function comment(id: number, authorId: number, parentId: number | null = null): Comment {
  return {
    id,
    activityId: null,
    parentId,
    author: { id: authorId, fullName: `Người ${authorId}`, avatarUrl: null },
    content: `Bình luận ${id}`,
    createdAt: '2026-10-19T03:00:00Z',
  }
}

describe('canDeleteComment', () => {
  it('lets the author delete their own comment, whatever their role', () => {
    expect(canDeleteComment(comment(1, 5), 5, 'VIEWER')).toBe(true)
    expect(canDeleteComment(comment(1, 5), 5, 'EDITOR')).toBe(true)
  })

  it('lets the owner delete a comment of anybody', () => {
    expect(canDeleteComment(comment(1, 5), 9, 'OWNER')).toBe(true)
  })

  it('does not let an editor or a viewer delete what someone else wrote', () => {
    expect(canDeleteComment(comment(1, 5), 9, 'EDITOR')).toBe(false)
    expect(canDeleteComment(comment(1, 5), 9, 'VIEWER')).toBe(false)
  })

  it('offers nothing while the signed-in account is not known, except to the owner', () => {
    expect(canDeleteComment(comment(1, 5), null, 'VIEWER')).toBe(false)
    expect(canDeleteComment(comment(1, 5), null, 'OWNER')).toBe(true)
  })
})

describe('withoutDeleted', () => {
  it('drops a root comment together with its replies, and nothing else', () => {
    const comments = [comment(1, 5), comment(2, 6, 1), comment(3, 5), comment(4, 7, 1), comment(5, 6, 3)]
    expect(withoutDeleted(comments, 1).map((c) => c.id)).toEqual([3, 5])
  })

  it('drops only the reply when a reply is deleted', () => {
    const comments = [comment(1, 5), comment(2, 6, 1), comment(4, 7, 1)]
    expect(withoutDeleted(comments, 2).map((c) => c.id)).toEqual([1, 4])
  })
})
