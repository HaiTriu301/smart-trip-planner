import { describe, expect, it } from 'vitest'
import { openingDayIndex } from './tripDates'

const DAYS = [
  { dayIndex: 1, date: '2026-10-12' },
  { dayIndex: 2, date: '2026-10-13' },
  { dayIndex: 3, date: '2026-10-14' },
]

describe('openingDayIndex', () => {
  it('opens the day it is today while the trip is in progress', () => {
    expect(openingDayIndex(DAYS, '2026-10-13')).toBe(2)
  })

  it.each([
    ['the first day', '2026-10-12', 1],
    ['the last day', '2026-10-14', 3],
  ])('counts %s of the trip as in progress', (_name, today, expected) => {
    expect(openingDayIndex(DAYS, today)).toBe(expected)
  })

  it.each([
    ['the day before the trip starts', '2026-10-11'],
    ['the day after the trip ends', '2026-10-15'],
    ['long before', '2025-01-01'],
    ['long after', '2027-06-30'],
  ])('opens day 1 on %s', (_name, today) => {
    expect(openingDayIndex(DAYS, today)).toBe(1)
  })
})
