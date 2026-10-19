import { describe, expect, it } from 'vitest'
import type { Activity } from '../types/activity'
import type { Comment } from '../types/comment'
import type { TripDayDetail } from '../types/trip'
import {
  activitiesById,
  activitiesUnderDiscussion,
  commentCountsByActivity,
  commentsAbout,
  commentThreads,
  splitComments,
} from './commentTree'

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

describe('commentsAbout', () => {
  const about = (id: number, activityId: number | null, parentId: number | null = null): Comment => ({
    ...comment(id, parentId),
    activityId,
  })

  it('keeps the comments of one activity with their replies, in the order of the list', () => {
    const comments = [about(1, 7), about(2, null), about(3, 8), about(4, 7, 1), about(5, 7), about(6, 8, 3)]
    expect(commentsAbout(comments, 7).map((c) => c.id)).toEqual([1, 4, 5])
    expect(ids(commentThreads(commentsAbout(comments, 7)))).toEqual([
      [1, [4]],
      [5, []],
    ])
  })

  it('gives nothing for an activity nobody commented on', () => {
    expect(commentsAbout([about(1, 7), about(2, null)], 9)).toEqual([])
  })
})

describe('splitComments', () => {
  const about = (id: number, activityId: number | null, parentId: number | null = null): Comment => ({
    ...comment(id, parentId),
    activityId,
  })

  it('puts comments on the whole trip in one tab and comments on activities in the other, replies with their root', () => {
    const { trip, activities } = splitComments([
      about(1, null),
      about(2, 7),
      about(3, null, 1),
      about(4, 8),
      about(5, 7, 2),
      about(6, null),
    ])
    expect(trip.map((c) => c.id)).toEqual([1, 3, 6])
    expect(activities.map((c) => c.id)).toEqual([2, 4, 5])
  })

  it('leaves a tab empty when nobody wrote there', () => {
    expect(splitComments([about(1, 7)]).trip).toEqual([])
    expect(splitComments([about(1, null)]).activities).toEqual([])
    expect(splitComments([])).toEqual({ trip: [], activities: [] })
  })
})

describe('commentCountsByActivity', () => {
  const about = (id: number, activityId: number | null, parentId: number | null = null): Comment => ({
    ...comment(id, parentId),
    activityId,
  })

  it('counts the comments of each activity, replies included', () => {
    const counts = commentCountsByActivity([about(1, 7), about(2, 7, 1), about(3, 8), about(4, 7), about(5, 7, 4)])
    expect(counts.get(7)).toBe(4)
    expect(counts.get(8)).toBe(1)
  })

  it('leaves out comments on the whole trip, and activities without comments', () => {
    const counts = commentCountsByActivity([about(1, null), about(2, null, 1), about(3, 8)])
    expect(Array.from(counts.entries())).toEqual([[8, 1]])
    expect(counts.get(7)).toBeUndefined()
    expect(commentCountsByActivity([]).size).toBe(0)
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

describe('activitiesUnderDiscussion', () => {
  const about = (id: number, activityId: number | null, parentId: number | null = null): Comment => ({
    ...comment(id, parentId),
    activityId,
  })
  const titles = (discussed: ReturnType<typeof activitiesUnderDiscussion>) =>
    discussed.map(({ day, activities }) => [day.dayIndex, activities.map((a) => a.activity.title)])

  const days = [
    day(1, [activity(11, 'Chùa Linh Ứng'), activity(12, 'Bún chả cá 109'), activity(13, 'Cầu Rồng')]),
    day(2, [activity(21, 'Bà Nà Hills')]),
    day(3, [activity(31, 'Hội An'), activity(32, 'Chợ đêm')]),
  ]

  it('groups the activities with comments by day, in the order of the itinerary, not of the comments', () => {
    const comments = [about(1, 32), about(2, 13), about(3, 11), about(4, null), about(5, 31)]
    expect(titles(activitiesUnderDiscussion(comments, days))).toEqual([
      [1, ['Chùa Linh Ứng', 'Cầu Rồng']],
      [3, ['Hội An', 'Chợ đêm']],
    ])
  })

  it('counts replies and takes the newest comment, a reply included, for the preview', () => {
    const comments = [about(1, 12), about(2, 12, 1), about(3, 11), about(4, 12), about(5, 12, 4)]
    const [{ activities }] = activitiesUnderDiscussion(comments, days)
    const food = activities.find((a) => a.activity.id === 12)
    expect(food?.count).toBe(4)
    expect(food?.latest.id).toBe(5)
    expect(activities.find((a) => a.activity.id === 11)?.latest.id).toBe(3)
  })

  it('leaves out days without discussion and comments on an activity the trip no longer has', () => {
    expect(titles(activitiesUnderDiscussion([about(1, 21), about(2, 99)], days))).toEqual([[2, ['Bà Nà Hills']]])
    expect(activitiesUnderDiscussion([about(1, null)], days)).toEqual([])
    expect(activitiesUnderDiscussion([], days)).toEqual([])
  })

  it('follows the day numbers even when the days come in another order', () => {
    const comments = [about(1, 31), about(2, 11)]
    expect(titles(activitiesUnderDiscussion(comments, [days[2], days[0]]))).toEqual([
      [1, ['Chùa Linh Ứng']],
      [3, ['Hội An']],
    ])
  })
})

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
