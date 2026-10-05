// Mirrors backend dto/response/TripWeatherResponse (design.md 10.2 "Quy ước Weather API").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

/** Every value the server may send; the mock source produces only five of them. */
export type WeatherCondition = 'CLEAR' | 'PARTLY_CLOUDY' | 'CLOUDY' | 'FOG' | 'RAIN' | 'THUNDERSTORM' | 'SNOW'

/** The weather of one day. Temperatures are degrees Celsius with one decimal, the chance of rain is 0 to 100. */
export interface DailyForecast {
  condition: WeatherCondition
  tempMin: number
  tempMax: number
  precipitationProbability: number
}

/** One day of the trip; forecast is null when there is none for that day (past, or further than 16 days). */
export interface TripWeatherDay {
  dayId: number
  /** "YYYY-MM-DD" */
  date: string
  forecast: DailyForecast | null
}

/**
 * NO_DESTINATION: the trip has no destination position yet, so no day has a forecast.
 * UNAVAILABLE: the weather source did not answer this time; no day has a forecast, asking again may give one.
 */
export type TripWeatherStatus = 'OK' | 'NO_DESTINATION' | 'UNAVAILABLE'

/** GET /weather/trips/{tripId}: one element per day of the trip, in calendar order. */
export interface TripWeather {
  status: TripWeatherStatus
  days: TripWeatherDay[]
}
