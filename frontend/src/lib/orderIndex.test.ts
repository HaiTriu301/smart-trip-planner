import { describe, expect, it } from 'vitest'
import { ORDER_STEP, reorderItemsForDrop } from './orderIndex'

const DAY = 7

describe('reorderItemsForDrop', () => {
  it('takes the midpoint when dropped between two neighbours, and sends only the moved activity', () => {
    const day = [
      { id: 1, orderIndex: 1000 },
      { id: 9, orderIndex: 5000 }, // moved here, still carrying its old index
      { id: 2, orderIndex: 2000 },
    ]

    expect(reorderItemsForDrop(DAY, day, 9)).toEqual([{ activityId: 9, dayId: DAY, orderIndex: 1500 }])
  })

  it('goes one step after the last activity when dropped at the end', () => {
    const day = [
      { id: 1, orderIndex: 1000 },
      { id: 2, orderIndex: 2000 },
      { id: 9, orderIndex: 10 },
    ]

    expect(reorderItemsForDrop(DAY, day, 9)).toEqual([{ activityId: 9, dayId: DAY, orderIndex: 2000 + ORDER_STEP }])
  })

  it('goes halfway between zero and the first activity when dropped at the start', () => {
    const day = [
      { id: 9, orderIndex: 3000 },
      { id: 1, orderIndex: 1000 },
    ]

    expect(reorderItemsForDrop(DAY, day, 9)).toEqual([{ activityId: 9, dayId: DAY, orderIndex: 500 }])
  })

  it('gives the first step to an activity dropped into an empty day', () => {
    expect(reorderItemsForDrop(DAY, [{ id: 9, orderIndex: 4000 }], 9)).toEqual([
      { activityId: 9, dayId: DAY, orderIndex: ORDER_STEP },
    ])
  })

  it('renumbers the whole day when no free index is left between the neighbours', () => {
    const day = [
      { id: 1, orderIndex: 5 },
      { id: 9, orderIndex: 9000 },
      { id: 2, orderIndex: 6 },
    ]

    expect(reorderItemsForDrop(DAY, day, 9)).toEqual([
      { activityId: 1, dayId: DAY, orderIndex: 1000 },
      { activityId: 9, dayId: DAY, orderIndex: 2000 },
      { activityId: 2, dayId: DAY, orderIndex: 3000 },
    ])
  })

  it('renumbers the whole day when the first activity already sits at the lowest index', () => {
    const day = [
      { id: 9, orderIndex: 2000 },
      { id: 1, orderIndex: 1 },
    ]

    expect(reorderItemsForDrop(DAY, day, 9)).toEqual([
      { activityId: 9, dayId: DAY, orderIndex: 1000 },
      { activityId: 1, dayId: DAY, orderIndex: 2000 },
    ])
  })

  it('renumbers the whole day when one more step would pass the highest index', () => {
    const day = [
      { id: 1, orderIndex: 1_000_000_000 },
      { id: 9, orderIndex: 1000 },
    ]

    expect(reorderItemsForDrop(DAY, day, 9)).toEqual([
      { activityId: 1, dayId: DAY, orderIndex: 1000 },
      { activityId: 9, dayId: DAY, orderIndex: 2000 },
    ])
  })
})
