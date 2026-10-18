import { DEFAULT_TIME_ZONE } from './today'

/** Milliseconds the zone is ahead of UTC at the instant `utcMs` (negative west of Greenwich). */
function offsetAt(utcMs: number, timeZone: string): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).formatToParts(new Date(utcMs))
  const part = (type: string) => Number(parts.find((p) => p.type === type)?.value)
  const wallClock = Date.UTC(part('year'), part('month') - 1, part('day'), part('hour'), part('minute'), part('second'))
  return wallClock - Math.floor(utcMs / 1000) * 1000
}

/** The instant midnight starts `year-month-day` in the zone; the offset is read twice in case it changes that day. */
function midnightIn(year: number, month: number, day: number, timeZone: string): number {
  const asIfUtc = Date.UTC(year, month - 1, day)
  const first = asIfUtc - offsetAt(asIfUtc, timeZone)
  return asIfUtc - offsetAt(first, timeZone)
}

/**
 * When a share link chosen to work "until 20/10/2026" stops (Task 4.5 decision 5): the last millisecond of that
 * day in the account's time zone, as the ISO Instant the server expects for expiresAt. The owner thinks in days,
 * the server in instants; the account's zone decides where the day ends, as everywhere in the app
 * (design.md rule 14.22). A zone the browser does not know falls back to the default one.
 *
 * @param date "YYYY-MM-DD" from a date field
 */
export function expiryInstant(date: string, timeZone: string): string {
  const [year, month, day] = date.split('-').map(Number)
  let nextMidnight: number
  try {
    nextMidnight = midnightIn(year, month, day + 1, timeZone)
  } catch {
    nextMidnight = midnightIn(year, month, day + 1, DEFAULT_TIME_ZONE)
  }
  return new Date(nextMidnight - 1).toISOString()
}
