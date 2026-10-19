import { describe, expect, it } from 'vitest'
import type { Comment } from '../types/comment'
import { commentThreads } from './commentTree'

function comment(id: number, parentId: number | null = null): Comment {
  return {
    id,
    activityId: null,
    parentId,
    author: { id: 1, fullName: 'Mai Anh', avatarUrl: null },
    content: `Bình luận ${id}`,
    createdAt: `2026-10-19T0${id}:00:00Z`,
  }
}

const ids = (threads: ReturnType<typeof commentThreads>) =>
  threads.map((thread) => [thread.comment.id, thread.replies.map((reply) => reply.id)])

describe('commentThreads', () => {
  it('puts every reply under its root and keeps the order of the list, roots and replies alike', () => {
    const threads = commentThreads([comment(1), comment(2), comment(3, 1), comment(4), comment(5, 1), comment(6, 2)])
    expect(ids(threads)).toEqual([
      [1, [3, 5]],
      [2, [6]],
      [4, []],
    ])
  })

  it('leaves out a reply whose root is not in the list, or whose parent is itself a reply', () => {
    const threads = commentThreads([comment(1), comment(2, 99), comment(3, 1), comment(4, 3)])
    expect(ids(threads)).toEqual([[1, [3]]])
  })

  it('gives no thread for an empty list', () => {
    expect(commentThreads([])).toEqual([])
  })
})
