import { describe, expect, it } from 'vitest'
import { formatDuration, minutesBetween } from './duration'

describe('minutesBetween', () => {
  it('counts the minutes from the start to a later end', () => {
    expect(minutesBetween('09:00', '10:30')).toBe(90)
    expect(minutesBetween('23:30', '23:45')).toBe(15)
  })

  it('gives null when the end is not after the start', () => {
    expect(minutesBetween('10:00', '10:00')).toBeNull()
    expect(minutesBetween('10:00', '09:00')).toBeNull()
  })

  it('gives null when either side is not a time', () => {
    expect(minutesBetween('', '10:00')).toBeNull()
    expect(minutesBetween('09:00', '930')).toBeNull()
  })
})

describe('formatDuration', () => {
  it('writes minutes, hours, or both', () => {
    expect(formatDuration(15)).toBe('15 phút')
    expect(formatDuration(60)).toBe('1 giờ')
    expect(formatDuration(90)).toBe('1 giờ 30 phút')
    expect(formatDuration(150)).toBe('2 giờ 30 phút')
  })
})
