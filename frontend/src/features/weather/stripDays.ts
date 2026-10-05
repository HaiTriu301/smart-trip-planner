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

// Pages (decided 2026-10-05): the forecast never scrolls sideways. When the tiles do not all fit, the card shows
// as many as fit and two buttons in its heading turn a whole page at a time.

/** A tile narrower than this is too tight for "N12 · 13/10". Four of them fit the 400px column */
export const MIN_TILE_WIDTH = 88
/** Space between two tiles, in pixels */
export const TILE_GAP = 8

export interface StripLayout {
  /** Tiles on one page, at least one */
  size: number
  /** False when every tile fits at once: no buttons then, and the tiles share the whole width */
  paged: boolean
}

/**
 * How `total` tiles are laid out in a row `width` pixels wide. The buttons sit in the heading of the card and
 * take nothing from the row, so a page holds as many tiles as the row does.
 */
export function stripLayout(width: number, total: number): StripLayout {
  // n tiles need n widths and n - 1 gaps
  const fits = Math.max(1, Math.floor((width + TILE_GAP) / (MIN_TILE_WIDTH + TILE_GAP)))
  return total <= fits ? { size: Math.max(1, total), paged: false } : { size: fits, paged: true }
}

/** Number of pages needed for `total` cells, at least one. */
export function pageCount(total: number, size: number): number {
  return Math.max(1, Math.ceil(total / size))
}

/** The page, counted from 0, that holds the cell at `position` (also counted from 0). */
export function pageOf(position: number, size: number): number {
  return Math.floor(position / size)
}
