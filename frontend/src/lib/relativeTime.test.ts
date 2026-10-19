import { describe, expect, it } from 'vitest'
import { relativeTime } from './relativeTime'

const VN = 'Asia/Ho_Chi_Minh'
// 19/10/2026 10:00 in Vietnam (UTC+7)
const NOW = new Date('2026-10-19T03:00:00Z')

/** The moment `ms` milliseconds before NOW, as the server writes it */
const ago = (ms: number) => new Date(NOW.getTime() - ms).toISOString()
const SECOND = 1000
const MINUTE = 60 * SECOND
const HOUR = 60 * MINUTE

describe('relativeTime', () => {
  it('says "Vừa xong" under a minute, and for a moment slightly ahead of the browser clock', () => {
    expect(relativeTime(ago(0), NOW, VN)).toBe('Vừa xong')
    expect(relativeTime(ago(59 * SECOND), NOW, VN)).toBe('Vừa xong')
    expect(relativeTime(ago(-30 * SECOND), NOW, VN)).toBe('Vừa xong')
  })

  it('counts whole minutes under an hour', () => {
    expect(relativeTime(ago(MINUTE), NOW, VN)).toBe('1 phút trước')
    expect(relativeTime(ago(12 * MINUTE + 40 * SECOND), NOW, VN)).toBe('12 phút trước')
    expect(relativeTime(ago(HOUR - SECOND), NOW, VN)).toBe('59 phút trước')
  })

  it('counts whole hours under a day', () => {
    expect(relativeTime(ago(HOUR), NOW, VN)).toBe('1 giờ trước')
    expect(relativeTime(ago(24 * HOUR - MINUTE), NOW, VN)).toBe('23 giờ trước')
  })

  it('keeps counting hours across midnight: posted at 23:00, read at 00:30', () => {
    // 20/10 00:30 and 19/10 23:00 in Vietnam
    expect(relativeTime('2026-10-19T16:00:00Z', new Date('2026-10-19T17:30:00Z'), VN)).toBe('1 giờ trước')
  })

  it('says "Hôm qua lúc …" from 24 hours on when the day before is the day of the comment', () => {
    // 18/10 08:00 in Vietnam, 26 hours before
    expect(relativeTime('2026-10-18T01:00:00Z', NOW, VN)).toBe('Hôm qua lúc 08:00')
  })

  it('takes yesterday and the days ago from the account time zone, not the browser', () => {
    // 18/10 01:00 in Vietnam but still 17/10 18:00 in UTC
    expect(relativeTime('2026-10-17T18:00:00Z', NOW, VN)).toBe('Hôm qua lúc 01:00')
    expect(relativeTime('2026-10-17T18:00:00Z', NOW, 'UTC')).toBe('2 ngày trước')
  })

  it('counts calendar days from 2 to 6 days ago', () => {
    expect(relativeTime('2026-10-15T07:30:00Z', NOW, VN)).toBe('4 ngày trước')
    expect(relativeTime('2026-10-13T07:30:00Z', NOW, VN)).toBe('6 ngày trước')
  })

  it('writes the day, month and time from 7 days on within the same year', () => {
    expect(relativeTime('2026-10-12T07:30:00Z', NOW, VN)).toBe('12/10 lúc 14:30')
    expect(relativeTime('2026-01-02T01:05:00.123456Z', NOW, VN)).toBe('02/01 lúc 08:05')
  })

  it('writes the full date, without the time, for another year', () => {
    expect(relativeTime('2025-12-30T07:30:00Z', NOW, VN)).toBe('30/12/2025')
  })

  it('still counts days across new year when it is less than a week', () => {
    // 31/12/2025 14:30 read on 02/01/2026 10:00 in Vietnam
    expect(relativeTime('2025-12-31T07:30:00Z', new Date('2026-01-02T03:00:00Z'), VN)).toBe('2 ngày trước')
  })
})
