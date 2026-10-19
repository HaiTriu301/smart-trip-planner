import { describe, expect, it } from 'vitest'
import type { Activity } from '../types/activity'
import type { Comment } from '../types/comment'
import type { TripDayDetail } from '../types/trip'
import { activitiesById, commentThreads } from './commentTree'

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

function activity(id: number, title: string): Activity {
  return {
    id,
    dayId: 1,
    title,
    type: 'FOOD',
    startTime: null,
    endTime: null,
    orderIndex: id,
    note: null,
    costAmount: null,
    currency: null,
    bookingUrl: null,
    place: null,
    createdById: 1,
    version: 0,
    createdAt: '2026-10-19T00:00:00Z',
    updatedAt: '2026-10-19T00:00:00Z',
  }
}

function day(dayIndex: number, activities: Activity[]): TripDayDetail {
  return { id: dayIndex, dayIndex, date: `2026-10-${10 + dayIndex}`, title: null, note: null, activities }
}

describe('activitiesById', () => {
  it('finds an activity of any day by its id', () => {
    const byId = activitiesById([
      day(1, [activity(11, 'Chùa Linh Ứng'), activity(12, 'Bún chả cá 109')]),
      day(2, []),
      day(3, [activity(31, 'Cầu Rồng')]),
    ])
    expect(byId.get(12)?.title).toBe('Bún chả cá 109')
    expect(byId.get(31)?.title).toBe('Cầu Rồng')
    expect(byId.size).toBe(3)
  })

  it('knows nothing of an activity the trip does not have, so its comment gets no chip', () => {
    expect(activitiesById([day(1, [activity(11, 'Chùa Linh Ứng')])]).get(99)).toBeUndefined()
    expect(activitiesById([]).size).toBe(0)
  })
})
