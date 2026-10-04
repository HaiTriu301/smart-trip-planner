import type { CardForecastTarget } from './cardForecast'
import { formatDegrees, WEATHER_CONDITIONS } from './weatherCondition'
import { useTripWeather } from './useTripWeather'

interface TripCardWeatherProps {
  tripId: number
  /** The day to show; the card decides it from the dates and mounts this component only when there is one */
  target: CardForecastTarget
}

/**
 * The weather on a trip card (UI_GUIDE 8.2): "Hôm nay" for a trip in progress, "Ngày đi" for one about to
 * start, then the icon and the high / low. It asks for the forecast of its trip, the same request the trip
 * page uses, so opening the trip afterwards finds it already loaded.
 * <p>
 * An extra: nothing is drawn while loading, on failure, for a trip without a destination position or a day
 * without a forecast. The card never waits for it and never shows an error because of it.
 */
export function TripCardWeather({ tripId, target }: TripCardWeatherProps) {
  const { data: weather } = useTripWeather(tripId)
  const forecast = weather?.days.find((day) => day.date === target.date)?.forecast
  if (!forecast) return null

  const look = WEATHER_CONDITIONS[forecast.condition]
  return (
    <span className="tabular ml-auto inline-flex items-center gap-1.5 whitespace-nowrap">
      {target.kind === 'today' ? 'Hôm nay' : 'Ngày đi'}
      <look.Icon aria-hidden className={`size-4 shrink-0 ${look.color}`} />
      <span className="sr-only">{look.label},</span>
      {formatDegrees(forecast.tempMax)} / {formatDegrees(forecast.tempMin)}
    </span>
  )
}
