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

const WEEKDAYS = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy']

/** "2026-10-01" → "Thứ năm" */
export function formatWeekday(isoDate: string): string {
  return WEEKDAYS[new Date(toUtcMillis(isoDate)).getUTCDay()]
}

/** 5000000, "VND" → "5.000.000 ₫" */
export function formatMoney(amount: number, currency: string): string {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(amount)
}

function toUtcMillis(isoDate: string): number {
  const [year, month, day] = isoDate.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}
