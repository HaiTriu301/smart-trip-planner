import { describe, expect, it } from 'vitest'
import { formatDistance, formatDuration } from './format'

describe('formatDistance', () => {
  it.each([
    [1, '1 m'],
    [998, '998 m'],
    [999, '999 m'],
    [1000, '1,0 km'],
    [3200, '3,2 km'],
    [8440, '8,4 km'],
    [8460, '8,5 km'],
    [125_300, '125,3 km'],
  ])('%i m → %s', (meters, expected) => {
    expect(formatDistance(meters)).toBe(expected)
  })
})

describe('formatDuration', () => {
  it.each([
    [1, '1 phút'],
    [59, '1 phút'],
    [60, '1 phút'],
    [61, '2 phút'],
    [120, '2 phút'],
    [1500, '25 phút'],
    [3540, '59 phút'],
    [3541, '1 giờ'],
    [3600, '1 giờ'],
    [3900, '1 giờ 5 phút'],
    [7260, '2 giờ 1 phút'],
  ])('%i s → %s', (seconds, expected) => {
    expect(formatDuration(seconds)).toBe(expected)
  })

  it('never says 0 minutes for a leg that exists', () => {
    expect(formatDuration(0)).toBe('1 phút')
  })
})
