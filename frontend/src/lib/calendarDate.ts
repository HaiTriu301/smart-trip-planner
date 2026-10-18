// The calendar of a date field works with Date objects, the form and the server with "YYYY-MM-DD" (LocalDate).
// The two meet here, on the computer's own calendar: new Date("2026-03-12") would read UTC midnight and show
// 11 March west of Greenwich (WORKFLOW.md Task 4.9 trap 4). "Today" still comes from useToday (account time zone).

import { countDays } from './format'

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/

/** "2026-03-12" → 12 March 2026, 00:00 on this computer; undefined for an empty or broken value. */
export function toCalendarDate(isoDate: string): Date | undefined {
  const match = ISO_DATE.exec(isoDate)
  if (!match) return undefined
  const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])]
  const date = new Date(year, month - 1, day)
  // 2026-02-30 would roll over to 2 March: not a date
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return undefined
  return date
}

/** The day a calendar cell stands for, as "YYYY-MM-DD", whatever the hour inside the Date. */
export function fromCalendarDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** "2026-03-12" plus 4 days → "2026-03-16", across months, years and leap days. */
export function addDays(isoDate: string, days: number): string {
  const date = toCalendarDate(isoDate)
  if (!date) return isoDate
  date.setDate(date.getDate() + days)
  return fromCalendarDate(date)
}

/** Under the trip dates: "5 ngày 4 đêm", "1 ngày" for a day trip; null until both dates are there and in order. */
export function stayLength(startDate: string, endDate: string): string | null {
  if (!toCalendarDate(startDate) || !toCalendarDate(endDate) || endDate < startDate) return null
  const days = countDays(startDate, endDate)
  return days === 1 ? '1 ngày' : `${days} ngày ${days - 1} đêm`
}
