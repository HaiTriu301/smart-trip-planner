// Times typed by hand in the activity form (Task 4.9 decision 4): the form keeps "HH:mm" (LocalTime on the server),
// but people type "9", "930" or "21h". Plain strings only, no Date: a time of day has no time zone.

/** "HH:mm", 24-hour, as the server reads it */
export const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/

const pad = (n: number) => String(n).padStart(2, '0')

function toTime(hours: number, minutes: number): string | null {
  if (hours > 23 || minutes > 59) return null
  return `${pad(hours)}:${pad(minutes)}`
}

/**
 * What a typed time means: "" for an empty field, "HH:mm" for a time, null when it is not one.
 * - digits only: 1–2 are the hour ("9" → 09:00), 3 are H + MM ("930" → 09:30), 4 are HH + MM ("2130" → 21:30);
 * - an hour, then ":", "." or "h", then optionally two digits of minutes ("9:30", "9.30", "9h30", "21h").
 * Minutes after a separator must have two digits: "9:5" could mean 09:05 or 09:50, so it is refused.
 */
export function parseTimeInput(text: string): string | null {
  const value = text.trim().toLowerCase()
  if (value === '') return ''
  const digits = /^\d{1,4}$/.exec(value)
  if (digits) {
    if (value.length <= 2) return toTime(Number(value), 0)
    const split = value.length - 2
    return toTime(Number(value.slice(0, split)), Number(value.slice(split)))
  }
  const separated = /^(\d{1,2})\s*[:.h]\s*(\d{2})?$/.exec(value)
  if (separated) return toTime(Number(separated[1]), Number(separated[2] ?? '0'))
  return null
}

/** Every time of the day, stepMinutes apart: 00:00, 00:15 … 23:45 */
export function timeSlots(stepMinutes = 15): string[] {
  const slots: string[] = []
  for (let minutes = 0; minutes < 24 * 60; minutes += stepMinutes) {
    slots.push(toTime(Math.floor(minutes / 60), minutes % 60) as string)
  }
  return slots
}

/** Index of the first slot at or after a time ("09:20" → the slot "09:30"); -1 when none is left that day. */
export function slotIndexFrom(slots: readonly string[], time: string): number {
  // "HH:mm" strings compare correctly as text
  return slots.findIndex((slot) => slot >= time)
}
