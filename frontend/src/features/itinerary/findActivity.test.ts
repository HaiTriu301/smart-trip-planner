import { describe, expect, it } from 'vitest'
import type { Activity } from '../../types/activity'
import type { TripDayDetail } from '../../types/trip'
import { findActivity } from './findActivity'

describe('findActivity', () => {
  it('finds the activity in any day, with the version the server has now', () => {
    const days = [day(1, [activity(10, 0)]), day(2, [activity(20, 3), activity(21, 0)])]

    expect(findActivity(days, 20)).toMatchObject({ id: 20, version: 3 })
    expect(findActivity(days, 10)).toMatchObject({ id: 10, dayId: 1 })
  })

  it('follows an activity that somebody moved to another day', () => {
    const before = [day(1, [activity(10, 0)]), day(2, [])]
    const after = [day(1, []), day(2, [{ ...activity(10, 0), dayId: 2 }])]

    expect(findActivity(before, 10)?.dayId).toBe(1)
    expect(findActivity(after, 10)?.dayId).toBe(2)
  })

  it('is undefined when the activity was deleted, or the trip has no day left', () => {
    expect(findActivity([day(1, [activity(10, 0)])], 11)).toBeUndefined()
    expect(findActivity([day(1, [])], 10)).toBeUndefined()
    expect(findActivity([], 10)).toBeUndefined()
  })
})

function activity(id: number, version: number): Activity {
  return {
    id,
    dayId: 1,
    title: `Hoạt động ${id}`,
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
    version,
    createdAt: '2026-10-29T00:00:00Z',
    updatedAt: '2026-10-29T00:00:00Z',
  }
}

/** The activities keep the dayId they were built with: the moved case sets it explicitly. */
function day(dayIndex: number, activities: Activity[]): TripDayDetail {
  return { id: dayIndex, dayIndex, date: `2026-10-${10 + dayIndex}`, title: null, note: null, activities }
}
