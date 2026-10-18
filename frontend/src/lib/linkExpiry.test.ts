import { describe, expect, it } from 'vitest'
import { expiryDateFor, expiryInstant } from './linkExpiry'

describe('expiryInstant', () => {
  it.each([
    // Vietnam, UTC+7 all year: 23:59:59.999 on the 20th is 16:59:59.999 UTC
    ['2026-10-20', 'Asia/Ho_Chi_Minh', '2026-10-20T16:59:59.999Z'],
    ['2026-10-20', 'UTC', '2026-10-20T23:59:59.999Z'],
    // Half-hour offset (UTC+5:30)
    ['2026-10-20', 'Asia/Kolkata', '2026-10-20T18:29:59.999Z'],
    // West of Greenwich the day ends after midnight UTC
    ['2026-10-20', 'America/Los_Angeles', '2026-10-21T06:59:59.999Z'],
  ])('ends %s in %s at %s', (date, zone, expected) => {
    expect(expiryInstant(date, zone)).toBe(expected)
  })

  it.each([
    ['the last day of a month', '2026-10-31', '2026-10-31T16:59:59.999Z'],
    ['the last day of the year', '2026-12-31', '2026-12-31T16:59:59.999Z'],
    ['29 February of a leap year', '2028-02-29', '2028-02-29T16:59:59.999Z'],
  ])('rolls over correctly on %s', (_case, date, expected) => {
    expect(expiryInstant(date, 'Asia/Ho_Chi_Minh')).toBe(expected)
  })

  it.each([
    // Summer time starts on 29 March 2026 in Berlin: the day ends at UTC+2, not UTC+1
    ['summer time starts that day', '2026-03-29', 'Europe/Berlin', '2026-03-29T21:59:59.999Z'],
    // Summer time ends on 1 November 2026 in New York: the day ends at UTC-5, not UTC-4
    ['summer time ends that day', '2026-11-01', 'America/New_York', '2026-11-02T04:59:59.999Z'],
  ])('uses the offset of the end of the day when %s', (_case, date, zone, expected) => {
    expect(expiryInstant(date, zone)).toBe(expected)
  })

  it('falls back to Vietnam time for a zone the browser does not know', () => {
    expect(expiryInstant('2026-10-20', 'Not/AZone')).toBe('2026-10-20T16:59:59.999Z')
  })
})

describe('expiryDateFor', () => {
  it('gives no date for a link that never expires', () => {
    expect(expiryDateFor('never', '2026-10-18', '')).toBe('')
  })

  it('counts the quick picks from today, across the end of a month', () => {
    expect(expiryDateFor('7', '2026-10-18', '')).toBe('2026-10-25')
    expect(expiryDateFor('30', '2026-10-18', '')).toBe('2026-11-17')
    expect(expiryDateFor('7', '2026-12-28', '')).toBe('2027-01-04')
  })

  it('takes the day picked on the calendar, or none yet', () => {
    expect(expiryDateFor('custom', '2026-10-18', '2026-12-24')).toBe('2026-12-24')
    expect(expiryDateFor('custom', '2026-10-18', '')).toBe('')
  })

  it('ignores a picked day once a quick pick is chosen again', () => {
    expect(expiryDateFor('7', '2026-10-18', '2026-12-24')).toBe('2026-10-25')
  })
})
