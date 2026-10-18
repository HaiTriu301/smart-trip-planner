import { TIME_PATTERN } from './timeInput'

const toMinutes = (time: string) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3))

/** Minutes from start to end ("HH:mm", same day); null unless both are times and the end comes after the start. */
export function minutesBetween(start: string, end: string): number | null {
  if (!TIME_PATTERN.test(start) || !TIME_PATTERN.test(end)) return null
  const minutes = toMinutes(end) - toMinutes(start)
  return minutes > 0 ? minutes : null
}

/** 15 → "15 phút", 60 → "1 giờ", 90 → "1 giờ 30 phút" (next to the end time of an activity). */
export function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) return `${rest} phút`
  if (rest === 0) return `${hours} giờ`
  return `${hours} giờ ${rest} phút`
}
