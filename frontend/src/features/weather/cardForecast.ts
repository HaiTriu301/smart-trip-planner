// Which day's forecast a trip card shows (design.md rule 14.20, decided 2026-10-05).

/** Forecasts exist for today and the 15 days after it: 16 days in all (WeatherService on the server). */
const FORECAST_DAYS = 16

export interface CardForecastTarget {
  /** "YYYY-MM-DD", one of the days of the trip */
  date: string
  /** today: the trip is in progress · departure: it starts within the forecast window */
  kind: 'today' | 'departure'
}

interface TripDates {
  startDate: string
  endDate: string
}

/** "2026-10-13" + 15 → "2026-10-28". Plain calendar arithmetic in UTC, no time zone involved. */
function addDays(isoDate: string, days: number): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10)
}

/**
 * The day whose forecast belongs on the card, or null when the card shows no weather: a trip in progress shows
 * today, a trip that starts within the forecast window shows its first day. A trip that is over, or one too far
 * ahead, has nothing worth asking the server for.
 */
export function cardForecastTarget(trip: TripDates, today: string): CardForecastTarget | null {
  if (trip.endDate < today) return null
  if (trip.startDate <= today) return { date: today, kind: 'today' }
  const lastForecastDay = addDays(today, FORECAST_DAYS - 1)
  return trip.startDate <= lastForecastDay ? { date: trip.startDate, kind: 'departure' } : null
}
