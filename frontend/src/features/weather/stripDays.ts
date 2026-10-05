import type { DailyForecast, TripWeatherDay } from '../../types/weather'

// What the weather strip under the day map shows (UI_GUIDE 9, decided 2026-10-05 after the first look in the
// browser): only the days that have a forecast. A day that is over, or further ahead than the forecast
// reaches, would be an empty cell that says nothing.

/** A day of the trip that has a forecast */
export interface ForecastDay extends TripWeatherDay {
  forecast: DailyForecast
}

/** The days the strip shows, in the order of the trip. */
export function daysWithForecast(days: readonly TripWeatherDay[]): ForecastDay[] {
  return days.filter((day): day is ForecastDay => day.forecast !== null)
}

/**
 * Why a trip with a destination has no forecast on any of its days, so the strip can say it:
 * over  – every day of the trip is behind today
 * ahead – the trip, or what is left of it, lies beyond the days a forecast exists for
 */
export type NoForecastReason = 'over' | 'ahead'

export function noForecastReason(days: readonly { date: string }[], today: string): NoForecastReason {
  return days.every((day) => day.date < today) ? 'over' : 'ahead'
}

// Pages (decided 2026-10-05): the strip never scrolls sideways. When the cells do not all fit, it shows as many
// as fit and two buttons turn a whole page at a time.

/**
 * A cell narrower than this cannot hold "32° / 25°" on one line. Chosen so the 420px column shows four days a
 * page with the buttons in place, the same four it shows when they all fit
 */
export const MIN_CELL_WIDTH = 88
/** Room taken by the two page buttons together */
export const PAGE_BUTTONS_WIDTH = 64

export interface StripLayout {
  /** Cells on one page, at least one */
  size: number
  /** False when every cell fits at once: no buttons then, and the cells share the whole width */
  paged: boolean
}

/** How `total` cells are laid out in a strip `width` pixels wide. */
export function stripLayout(width: number, total: number): StripLayout {
  if (total <= Math.floor(width / MIN_CELL_WIDTH)) return { size: Math.max(1, total), paged: false }
  return { size: Math.max(1, Math.floor((width - PAGE_BUTTONS_WIDTH) / MIN_CELL_WIDTH)), paged: true }
}

/** Number of pages needed for `total` cells, at least one. */
export function pageCount(total: number, size: number): number {
  return Math.max(1, Math.ceil(total / size))
}

/** The page, counted from 0, that holds the cell at `position` (also counted from 0). */
export function pageOf(position: number, size: number): number {
  return Math.floor(position / size)
}
