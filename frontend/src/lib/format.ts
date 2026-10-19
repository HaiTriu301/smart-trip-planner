// Backend LocalDate values arrive as "YYYY-MM-DD". They are handled as plain strings on purpose:
// new Date("2026-10-01") parses as UTC midnight and can show the previous day in negative-offset timezones.

import { DEFAULT_TIME_ZONE, todayIn } from './today'

/** "2026-10-01" → "01/10/2026" */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

/**
 * "12/10": the day and month of a moment sent by the server (an ISO Instant such as "2026-10-12T17:30:00Z"), on
 * the calendar of the account's time zone, not the browser's. Used where the year is plain from the context
 * ("Đã mời 12/10").
 */
export function formatDayMonthIn(instant: string, timeZone: string): string {
  return formatDate(todayIn(timeZone, new Date(instant))).slice(0, 5)
}

function clockIn(timeZone: string, moment: Date): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(moment)
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? ''
  return `${part('hour')}:${part('minute')}`
}

/**
 * "14:30": the clock time of a moment sent by the server, in the account's time zone, always 24-hour. A zone name
 * the browser does not know falls back to the default zone, like todayIn.
 */
export function formatTimeIn(instant: string, timeZone: string): string {
  const moment = new Date(instant)
  try {
    return clockIn(timeZone, moment)
  } catch {
    return clockIn(DEFAULT_TIME_ZONE, moment)
  }
}

/** "12/10/2026 14:30": the full date and time of a moment in the account's time zone (the hover text of a comment). */
export function formatDateTimeIn(instant: string, timeZone: string): string {
  return `${formatDate(todayIn(timeZone, new Date(instant)))} ${formatTimeIn(instant, timeZone)}`
}

/** "01/10/2026 – 05/10/2026", or a single date for a one-day trip. */
export function formatDateRange(startDate: string, endDate: string): string {
  if (startDate === endDate) return formatDate(startDate)
  return `${formatDate(startDate)} – ${formatDate(endDate)}`
}

/** Number of calendar days, both ends included (design.md rule 14.1). */
export function countDays(startDate: string, endDate: string): number {
  const MS_PER_DAY = 86_400_000
  return (toUtcMillis(endDate) - toUtcMillis(startDate)) / MS_PER_DAY + 1
}

const WEEKDAYS = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy']

/** "2026-10-01" → "Thứ năm" */
export function formatWeekday(isoDate: string): string {
  return WEEKDAYS[new Date(toUtcMillis(isoDate)).getUTCDay()]
}

/** 5000000, "VND" → "5.000.000 ₫" */
export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(amount)
}

/** 998 → "998 m", 8440 → "8,4 km": metres under one kilometre, then kilometres with one decimal. */
export function formatDistance(meters: number): string {
  if (meters < 1000) return `${meters} m`
  return `${(meters / 1000).toFixed(1).replace('.', ',')} km`
}

/** 1500 → "25 phút", 3900 → "1 giờ 5 phút". Rounded up to the minute: nobody plans a day in seconds. */
export function formatDuration(seconds: number): string {
  const minutes = Math.max(1, Math.ceil(seconds / 60))
  if (minutes < 60) return `${minutes} phút`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours} giờ` : `${hours} giờ ${rest} phút`
}

function toUtcMillis(isoDate: string): number {
  const [year, month, day] = isoDate.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}
