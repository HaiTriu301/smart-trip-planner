// Backend LocalDate values arrive as "YYYY-MM-DD". They are handled as plain strings on purpose:
// new Date("2026-10-01") parses as UTC midnight and can show the previous day in negative-offset timezones.

/** "2026-10-01" → "01/10/2026" */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
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

function toUtcMillis(isoDate: string): number {
  const [year, month, day] = isoDate.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}
