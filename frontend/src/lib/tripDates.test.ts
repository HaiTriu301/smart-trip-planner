import { describe, expect, it } from 'vitest'
import { openingDayIndex, shouldAskToComplete } from './tripDates'

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

describe('shouldAskToComplete', () => {
  const today = '2026-10-16'

  it.each(['DRAFT', 'PLANNED', 'ONGOING'] as const)('asks about a %s trip that ended yesterday', (status) => {
    expect(shouldAskToComplete({ endDate: '2026-10-15', status }, today)).toBe(true)
  })

  it.each(['COMPLETED', 'ARCHIVED'] as const)('does not ask about a %s trip', (status) => {
    expect(shouldAskToComplete({ endDate: '2026-10-15', status }, today)).toBe(false)
  })

  it('does not ask on the last day of the trip: the day is not over yet', () => {
    expect(shouldAskToComplete({ endDate: today, status: 'ONGOING' }, today)).toBe(false)
  })

  it('does not ask about a trip still ahead', () => {
    expect(shouldAskToComplete({ endDate: '2026-11-02', status: 'PLANNED' }, today)).toBe(false)
  })

  it('asks about a trip that ended long ago', () => {
    expect(shouldAskToComplete({ endDate: '2025-12-31', status: 'DRAFT' }, today)).toBe(true)
  })
})
