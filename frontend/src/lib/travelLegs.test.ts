import { describe, expect, it } from 'vitest'
import { legsAfter } from './travelLegs'

const PLACE = { id: 1 }
const leg = (fromActivityId: number, toActivityId: number, distanceMeters = 998, durationSeconds = 120) => ({
  fromActivityId,
  toActivityId,
  distanceMeters,
  durationSeconds,
})

describe('legsAfter', () => {
  it('puts each leg under the activity it leaves from', () => {
    const day = [
      { id: 1, place: PLACE },
      { id: 2, place: PLACE },
      { id: 3, place: PLACE },
    ]
    const legs = [leg(1, 2), leg(2, 3, 3200, 384)]

    const shown = legsAfter(day, legs)

    expect([...shown.keys()]).toEqual([1, 2])
    expect(shown.get(2)).toEqual({ leg: legs[1], direct: true })
  })

  it('shows nothing for a day without legs', () => {
    expect(legsAfter([{ id: 1, place: PLACE }], []).size).toBe(0)
  })

  it('leaves out a leg of 0 metres: two activities at the same place', () => {
    const day = [
      { id: 1, place: PLACE },
      { id: 2, place: PLACE },
    ]

    expect(legsAfter(day, [leg(1, 2, 0, 0)]).size).toBe(0)
  })

  it('drops a leg whose two activities are no longer in that order after a drag', () => {
    // Loaded for 1 → 2 → 3, then card 3 was dragged to the top: 3 → 1 → 2
    const day = [
      { id: 3, place: PLACE },
      { id: 1, place: PLACE },
      { id: 2, place: PLACE },
    ]

    const shown = legsAfter(day, [leg(1, 2), leg(2, 3)])

    // 1 → 2 is still true; 2 → 3 now points up the list, and 3 → 1 has not been loaded yet
    expect([...shown.keys()]).toEqual([1])
  })

  it('drops the legs of an activity that left the day', () => {
    const day = [
      { id: 1, place: PLACE },
      { id: 3, place: PLACE },
    ]

    expect(legsAfter(day, [leg(1, 2), leg(2, 3)]).size).toBe(0)
  })

  it('drops a leg when one of its activities has just lost its place', () => {
    const day = [
      { id: 1, place: PLACE },
      { id: 2, place: null },
    ]

    expect(legsAfter(day, [leg(1, 2)]).size).toBe(0)
  })

  it('keeps a leg that passes an activity without a place, under the activity it leaves from', () => {
    // Chợ Hàn → Nghỉ trưa (no place) → Cầu Rồng → Bún chả cá: the server sends 1 → 3 and 3 → 4
    const day = [
      { id: 1, place: PLACE },
      { id: 2, place: null },
      { id: 3, place: PLACE },
      { id: 4, place: PLACE },
    ]
    const legs = [leg(1, 3, 3200, 720), leg(3, 4)]

    const shown = legsAfter(day, legs)

    expect([...shown.keys()]).toEqual([1, 3])
    expect(shown.get(1)).toEqual({ leg: legs[0], direct: false })
    expect(shown.get(3)).toEqual({ leg: legs[1], direct: true })
  })

  it('keeps a leg that passes several activities without a place', () => {
    const day = [
      { id: 1, place: PLACE },
      { id: 2, place: null },
      { id: 3, place: null },
      { id: 4, place: PLACE },
    ]

    expect(legsAfter(day, [leg(1, 4)]).get(1)?.direct).toBe(false)
  })

  it('drops a leg when an activity with a place now sits between its two ends', () => {
    // Loaded as 1 → 3, then activity 2 was given a place (or one was dragged in between)
    const day = [
      { id: 1, place: PLACE },
      { id: 2, place: PLACE },
      { id: 3, place: PLACE },
    ]

    expect(legsAfter(day, [leg(1, 3)]).size).toBe(0)
  })
})
