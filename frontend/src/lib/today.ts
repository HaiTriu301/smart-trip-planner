// "Today" for the signed-in account (design.md rule 14.22): the calendar day in the account's time zone, not in
// the browser's. The server decides with the same zone which days still get a forecast, so both must agree.
// Dates are "YYYY-MM-DD" strings, like every LocalDate of the API; in that form plain string comparison is
// calendar order.

/** The zone every account has until a settings screen lets the user choose one (users.timezone default). */
export const DEFAULT_TIME_ZONE = 'Asia/Ho_Chi_Minh'

function formatIn(timeZone: string, now: Date): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now)
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? ''
  return `${part('year')}-${part('month')}-${part('day')}`
}

/**
 * The calendar day it is at `now` in `timeZone` (an IANA name such as "Asia/Ho_Chi_Minh"), as "YYYY-MM-DD".
 * A name the browser does not know falls back to the default zone instead of breaking the page.
 */
export function todayIn(timeZone: string, now: Date = new Date()): string {
  try {
    return formatIn(timeZone, now)
  } catch {
    return formatIn(DEFAULT_TIME_ZONE, now)
  }
}

export type DayStatus = 'past' | 'today' | 'upcoming'

/** Where a day stands against today: a day is over once its date is before today (design.md rule 14.22). */
export function dayStatus(date: string, today: string): DayStatus {
  if (date < today) return 'past'
  return date === today ? 'today' : 'upcoming'
}
