import { Droplet } from 'lucide-react'
import { formatDegrees, WEATHER_CONDITIONS } from './weatherCondition'
import { useTripWeather } from './useTripWeather'

interface DayWeatherLineProps {
  tripId: number
  dayId: number
}

/**
 * The weather of one day on a single line, for the day heading of narrow screens (UI_GUIDE 8.1): there the
 * strip sits under the map, on a tab the user may never open. Wide screens have the strip next to the list and
 * do not show this line. Nothing is shown while loading, on failure or for a day without a forecast: the line
 * is an extra, not something the heading should make room for.
 */
export function DayWeatherLine({ tripId, dayId }: DayWeatherLineProps) {
  const { data: weather } = useTripWeather(tripId)
  const forecast = weather?.days.find((day) => day.dayId === dayId)?.forecast
  if (!forecast) return null

  const look = WEATHER_CONDITIONS[forecast.condition]
  const temperatures = `${formatDegrees(forecast.tempMax)} / ${formatDegrees(forecast.tempMin)}`

  return (
    <p
      aria-label={`Thời tiết: ${look.label}, cao nhất ${formatDegrees(forecast.tempMax)}, thấp nhất ${formatDegrees(forecast.tempMin)}, khả năng mưa ${forecast.precipitationProbability}%`}
      className="tabular flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[13px] leading-5 text-gray-600 lg:hidden"
    >
      <span className="inline-flex items-center gap-1.5">
        <look.Icon aria-hidden className={`size-4 shrink-0 ${look.color}`} />
        {look.label}
      </span>
      <span aria-hidden className="text-gray-300">
        ·
      </span>
      <span>{temperatures}</span>
      <span aria-hidden className="text-gray-300">
        ·
      </span>
      <span className="inline-flex items-center gap-1">
        <Droplet aria-hidden className="size-3.5 shrink-0 text-info" />
        {forecast.precipitationProbability}%
      </span>
    </p>
  )
}
