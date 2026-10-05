import { describe, expect, it } from 'vitest'
import { dayStatus, todayIn } from './today'

describe('todayIn', () => {
  it('gives the day in the account zone, not the day in UTC', () => {
    // 18:30 UTC on 12 October is already 01:30 on 13 October in Vietnam (UTC+7)
    const now = new Date('2026-10-12T18:30:00Z')

    expect(todayIn('Asia/Ho_Chi_Minh', now)).toBe('2026-10-13')
    expect(todayIn('UTC', now)).toBe('2026-10-12')
  })

  it('changes day exactly at midnight of the account zone', () => {
    expect(todayIn('Asia/Ho_Chi_Minh', new Date('2026-10-12T16:59:59Z'))).toBe('2026-10-12')
    expect(todayIn('Asia/Ho_Chi_Minh', new Date('2026-10-12T17:00:00Z'))).toBe('2026-10-13')
  })

  it('follows a zone behind UTC as well', () => {
    // 03:00 UTC on 1 January is still 31 December in Los Angeles (UTC-8)
    expect(todayIn('America/Los_Angeles', new Date('2027-01-01T03:00:00Z'))).toBe('2026-12-31')
  })

  it('pads the month and the day to two digits', () => {
    expect(todayIn('UTC', new Date('2026-03-05T12:00:00Z'))).toBe('2026-03-05')
  })

  it('falls back to the default zone for a zone name the browser does not know', () => {
    const now = new Date('2026-10-12T18:30:00Z')

    expect(todayIn('Mars/Olympus_Mons', now)).toBe('2026-10-13')
  })
})

describe('dayStatus', () => {
  const today = '2026-10-13'

  it.each([
    ['2026-10-12', 'past'],
    ['2026-10-13', 'today'],
    ['2026-10-14', 'upcoming'],
    // Across a month and a year: compared as dates, not as numbers of the day
    ['2026-09-30', 'past'],
    ['2025-12-31', 'past'],
    ['2026-11-01', 'upcoming'],
    ['2027-01-01', 'upcoming'],
  ])('%s is %s', (date, expected) => {
    expect(dayStatus(date, today)).toBe(expected)
  })
})
