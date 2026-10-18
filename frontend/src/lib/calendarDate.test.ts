import { describe, expect, it } from 'vitest'
import { addDays, fromCalendarDate, stayLength, toCalendarDate } from './calendarDate'

describe('toCalendarDate / fromCalendarDate', () => {
  it('turns a date into the same day on the local calendar and back', () => {
    const date = toCalendarDate('2026-03-12')
    expect(date?.getFullYear()).toBe(2026)
    expect(date?.getMonth()).toBe(2)
    expect(date?.getDate()).toBe(12)
    expect(fromCalendarDate(date as Date)).toBe('2026-03-12')
  })

  it('keeps the day whatever the hour inside the Date', () => {
    expect(fromCalendarDate(new Date(2026, 0, 5, 23, 59))).toBe('2026-01-05')
    expect(fromCalendarDate(new Date(2026, 0, 5, 0, 0))).toBe('2026-01-05')
  })

  it('refuses an empty value and days that do not exist', () => {
    expect(toCalendarDate('')).toBeUndefined()
    expect(toCalendarDate('2026-02-30')).toBeUndefined()
    expect(toCalendarDate('2026-13-01')).toBeUndefined()
    expect(toCalendarDate('12/03/2026')).toBeUndefined()
    expect(toCalendarDate('2028-02-29')).toBeDefined()
  })
})

describe('addDays', () => {
  it('moves across a month, a year and a leap day', () => {
    expect(addDays('2026-03-12', 4)).toBe('2026-03-16')
    expect(addDays('2026-03-30', 3)).toBe('2026-04-02')
    expect(addDays('2026-12-30', 3)).toBe('2027-01-02')
    expect(addDays('2028-02-28', 1)).toBe('2028-02-29')
  })
})

describe('stayLength', () => {
  it('counts days and nights, both ends included', () => {
    expect(stayLength('2026-03-12', '2026-03-16')).toBe('5 ngày 4 đêm')
    expect(stayLength('2026-03-12', '2026-03-13')).toBe('2 ngày 1 đêm')
  })

  it('says "1 ngày" for a day trip', () => {
    expect(stayLength('2026-03-12', '2026-03-12')).toBe('1 ngày')
  })

  it('gives null until both dates are there and in order', () => {
    expect(stayLength('', '2026-03-12')).toBeNull()
    expect(stayLength('2026-03-12', '')).toBeNull()
    expect(stayLength('2026-03-16', '2026-03-12')).toBeNull()
  })
})
