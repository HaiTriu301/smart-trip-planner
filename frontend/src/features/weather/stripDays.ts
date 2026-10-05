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
