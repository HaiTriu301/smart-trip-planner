// How long ago a comment was posted, the way people say it (WORKFLOW.md Task 4.6 decision 6). Calendar words
// ("hôm qua", "4 ngày trước", the date) follow the account's time zone, like every date of the app.

import { countDays, formatDate, formatTimeIn } from './format'
import { todayIn } from './today'

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

/**
 * "Vừa xong", "12 phút trước", "3 giờ trước", "Hôm qua lúc 14:30", "4 ngày trước", "12/10 lúc 14:30", or
 * "12/10/2025" for another year.
 * <p>
 * The hours come first: posted at 23:00 and read at 00:30 is "1 giờ trước", not "hôm qua". A moment slightly
 * ahead of `now` (the server clock runs a bit ahead of the browser's) is "Vừa xong" too.
 */
export function relativeTime(instant: string, now: Date, timeZone: string): string {
  const elapsed = now.getTime() - new Date(instant).getTime()
  if (elapsed < MINUTE) return 'Vừa xong'
  if (elapsed < HOUR) return `${Math.floor(elapsed / MINUTE)} phút trước`
  if (elapsed < DAY) return `${Math.floor(elapsed / HOUR)} giờ trước`

  const day = todayIn(timeZone, new Date(instant))
  const today = todayIn(timeZone, now)
  // countDays counts both ends: yesterday → today is 2
  const daysAgo = countDays(day, today) - 1
  const time = formatTimeIn(instant, timeZone)
  if (daysAgo <= 1) return `Hôm qua lúc ${time}`
  if (daysAgo < 7) return `${daysAgo} ngày trước`
  const date = formatDate(day)
  return day.slice(0, 4) === today.slice(0, 4) ? `${date.slice(0, 5)} lúc ${time}` : date
}
