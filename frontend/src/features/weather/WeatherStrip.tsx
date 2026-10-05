import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { CalendarClock, CalendarCheck, CloudAlert, Droplet, MapPinOff } from 'lucide-react'
import type { ReactNode } from 'react'
import { Skeleton } from '../../components/Skeleton'
import { useToday } from '../../hooks/useToday'
import { formatDate } from '../../lib/format'
import type { TripDay } from '../../types/trip'
import { daysWithForecast, noForecastReason, type ForecastDay } from './stripDays'
import { formatDegrees, WEATHER_CONDITIONS } from './weatherCondition'
import { useTripWeather } from './useTripWeather'

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
 * The forecast of the trip under the day map (UI_GUIDE 9): one cell per day that has a forecast, each a link to
 * its day. Days already over and days further ahead than the forecast reaches are left out; when that leaves
 * nothing, or the trip has no destination position, the strip says why in one line.
 * The box keeps one height whatever it holds: the map above it measures its own box only once.
 * <p>
 * The forecast is an extra: when it cannot be loaded the strip says so and the rest of the page works as usual.
 * A forecast loaded earlier stays on screen when a later reload fails (same rule as the trip itself, BUG-UI-005).
 */
export function WeatherStrip({ tripId, days, currentDayIndex }: WeatherStripProps) {
  const { data: weather, isPending, isFetching, refetch } = useTripWeather(tripId)
  const today = useToday()

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
  const shown = weather?.status === 'OK' ? daysWithForecast(weather.days) : []

  return (
    <section
      aria-label="Dự báo thời tiết"
      className={`${BOX_HEIGHT} overflow-hidden rounded-card border border-tide bg-white`}
    >
      {!weather && (
        <Note icon={<CloudAlert aria-hidden className="size-5 shrink-0 text-gray-400" />}>
          Tạm thời không có dự báo.{' '}
          <button
            type="button"
            disabled={isFetching}
            onClick={() => refetch()}
            className="rounded-control font-medium text-jade hover:underline focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50 pointer-coarse:min-h-11"
          >
            {isFetching ? 'Đang thử lại…' : 'Thử lại'}
          </button>
        </Note>
      )}
      {weather?.status === 'NO_DESTINATION' && (
        // Same wording as the note on the map of a trip without a destination (UI_GUIDE 9)
        <Note icon={<MapPinOff aria-hidden className="size-5 shrink-0 text-gray-400" />}>
          Đặt vị trí điểm đến trong "Sửa" chuyến đi để xem dự báo thời tiết.
        </Note>
      )}
      {weather?.status === 'OK' &&
        shown.length === 0 &&
        (noForecastReason(weather.days, today) === 'over' ? (
          <Note icon={<CalendarCheck aria-hidden className="size-5 shrink-0 text-gray-400" />}>
            Chuyến đi đã qua, không còn dự báo thời tiết.
          </Note>
        ) : (
          <Note icon={<CalendarClock aria-hidden className="size-5 shrink-0 text-gray-400" />}>
            Chưa có dự báo. Dự báo chỉ có cho 16 ngày tới, chuyến đi này bắt đầu sau đó.
          </Note>
        ))}
      {shown.length > 0 && (
        <ol ref={listRef} className="relative flex h-full divide-x divide-tide overflow-x-auto">
          {shown.map((day) => {
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

/** One line with an icon, for every state in which the strip has no cells to show. */
function Note({ icon, children }: { icon: ReactNode; children: ReactNode }) {
  return (
    <div className="flex h-full items-center gap-3 px-4">
      {icon}
      <p className="text-sm text-gray-600">{children}</p>
    </div>
  )
}

interface DayCellProps {
  tripId: number
  dayIndex: number
  day: ForecastDay
  current: boolean
}

function DayCell({ tripId, dayIndex, day, current }: DayCellProps) {
  const shortDate = formatDate(day.date).slice(0, 5)
  const forecast = day.forecast
  const look = WEATHER_CONDITIONS[forecast.condition]

  return (
    <Link
      to={`/trips/${tripId}/days/${dayIndex}`}
      aria-current={current ? 'page' : undefined}
      aria-label={`Ngày ${dayIndex}, ${shortDate}: ${look.label}, cao nhất ${formatDegrees(forecast.tempMax)}, thấp nhất ${formatDegrees(forecast.tempMin)}, khả năng mưa ${forecast.precipitationProbability}%`}
      className={`flex h-full flex-col items-center justify-center gap-0.5 px-2 text-center transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none focus-visible:ring-inset ${
        current ? 'bg-jade-light' : 'hover:bg-gray-50'
      }`}
    >
      <span className={`tabular text-xs ${current ? 'font-semibold text-jade-dark' : 'text-gray-500'}`}>
        N{dayIndex} · {shortDate}
      </span>
      <look.Icon aria-hidden className={`size-5 ${look.color}`} />
      <span className="tabular text-sm font-semibold text-ink">
        {formatDegrees(forecast.tempMax)} / {formatDegrees(forecast.tempMin)}
      </span>
      <span className="tabular inline-flex items-center gap-0.5 text-xs text-gray-600">
        <Droplet aria-hidden className="size-3 text-info" />
        {forecast.precipitationProbability}%
      </span>
    </Link>
  )
}
