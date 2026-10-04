import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { CloudOff, Droplet, MapPinOff } from 'lucide-react'
import { getTripWeather } from '../../api/weather'
import { Skeleton } from '../../components/Skeleton'
import { formatDate } from '../../lib/format'
import type { TripDay } from '../../types/trip'
import type { TripWeatherDay } from '../../types/weather'
import { formatDegrees, WEATHER_CONDITIONS } from './weatherCondition'

/** Same height while loading, with a forecast and without one (h-28 = 112px; DayTimeline counts on it) */
const BOX_HEIGHT = 'h-28'

interface WeatherStripProps {
  tripId: number
  /** The days of the trip, for the number each forecast belongs to */
  days: TripDay[]
  /** The day on screen: its cell is highlighted and kept in view */
  currentDayIndex: number
}

/**
 * The forecast of every day of the trip, one cell per day, under the day map (UI_GUIDE 9). A cell is a link to
 * its day. A trip without a destination position has no forecast at all: the strip says how to get one.
 * The box keeps one height whatever it holds: the map above it measures its own box only once.
 */
export function WeatherStrip({ tripId, days, currentDayIndex }: WeatherStripProps) {
  // Its own key, not under ['trip', id]: editing an activity must not ask for the forecast again
  const { data: weather, isPending } = useQuery({
    queryKey: ['weather', tripId],
    queryFn: () => getTripWeather(tripId),
  })

  const listRef = useRef<HTMLOListElement>(null)
  const currentRef = useRef<HTMLLIElement>(null)
  const loaded = weather !== undefined

  // Long trips scroll sideways: bring the cell of the day on screen to the middle. Only the strip moves;
  // scrollIntoView would also scroll the page when the strip is below the fold.
  useEffect(() => {
    const list = listRef.current
    const cell = currentRef.current
    if (!list || !cell) return
    list.scrollTo({ left: cell.offsetLeft - (list.clientWidth - cell.clientWidth) / 2 })
  }, [currentDayIndex, loaded])

  if (isPending) {
    return (
      <div role="status" aria-label="Đang tải dự báo thời tiết">
        <Skeleton className={`${BOX_HEIGHT} w-full rounded-card`} />
      </div>
    )
  }

  const dayIndexById = new Map(days.map((day) => [day.id, day.dayIndex]))

  return (
    <section
      aria-label="Dự báo thời tiết"
      className={`${BOX_HEIGHT} overflow-hidden rounded-card border border-tide bg-white`}
    >
      {weather?.status === 'NO_DESTINATION' && (
        // Same wording as the note on the map of a trip without a destination (UI_GUIDE 9)
        <div className="flex h-full items-center gap-3 px-4">
          <MapPinOff aria-hidden className="size-5 shrink-0 text-gray-400" />
          <p className="text-sm text-gray-600">
            Đặt vị trí điểm đến trong "Sửa" chuyến đi để xem dự báo thời tiết.
          </p>
        </div>
      )}
      {weather?.status === 'OK' && (
        <ol ref={listRef} className="relative flex h-full divide-x divide-tide overflow-x-auto">
          {weather.days.map((day) => {
            const dayIndex = dayIndexById.get(day.dayId)
            // A day the trip no longer has (the trip was just shortened, the forecast is being reloaded)
            if (dayIndex === undefined) return null
            const current = dayIndex === currentDayIndex
            return (
              <li key={day.dayId} ref={current ? currentRef : undefined} className="min-w-24 flex-1">
                <DayCell tripId={tripId} dayIndex={dayIndex} day={day} current={current} />
              </li>
            )
          })}
        </ol>
      )}
    </section>
  )
}

interface DayCellProps {
  tripId: number
  dayIndex: number
  day: TripWeatherDay
  current: boolean
}

function DayCell({ tripId, dayIndex, day, current }: DayCellProps) {
  const shortDate = formatDate(day.date).slice(0, 5)
  const forecast = day.forecast
  const look = forecast && WEATHER_CONDITIONS[forecast.condition]

  return (
    <Link
      to={`/trips/${tripId}/days/${dayIndex}`}
      aria-current={current ? 'page' : undefined}
      aria-label={
        forecast && look
          ? `Ngày ${dayIndex}, ${shortDate}: ${look.label}, cao nhất ${formatDegrees(forecast.tempMax)}, thấp nhất ${formatDegrees(forecast.tempMin)}, khả năng mưa ${forecast.precipitationProbability}%`
          : `Ngày ${dayIndex}, ${shortDate}: chưa có dự báo`
      }
      className={`flex h-full flex-col items-center justify-center gap-0.5 px-2 text-center transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none focus-visible:ring-inset ${
        current ? 'bg-jade-light' : 'hover:bg-gray-50'
      }`}
    >
      <span className={`tabular text-xs ${current ? 'font-semibold text-jade-dark' : 'text-gray-500'}`}>
        N{dayIndex} · {shortDate}
      </span>
      {forecast && look ? (
        <>
          <look.Icon aria-hidden className={`size-5 ${look.color}`} />
          <span className="tabular text-sm font-semibold text-ink">
            {formatDegrees(forecast.tempMax)} / {formatDegrees(forecast.tempMin)}
          </span>
          <span className="tabular inline-flex items-center gap-0.5 text-xs text-gray-600">
            <Droplet aria-hidden className="size-3 text-info" />
            {forecast.precipitationProbability}%
          </span>
        </>
      ) : (
        <>
          <CloudOff aria-hidden className="size-5 text-gray-400" />
          <span className="text-xs text-gray-500">Chưa có</span>
        </>
      )}
    </Link>
  )
}
