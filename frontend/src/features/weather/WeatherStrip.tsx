import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  CalendarClock,
  CalendarCheck,
  ChevronLeft,
  ChevronRight,
  CloudAlert,
  Droplet,
  MapPinOff,
} from 'lucide-react'
import { Skeleton } from '../../components/Skeleton'
import { useToday } from '../../hooks/useToday'
import { formatDate } from '../../lib/format'
import type { TripDay } from '../../types/trip'
import {
  daysWithForecast,
  noForecastReason,
  pageCount,
  pageOf,
  stripLayout,
  type ForecastDay,
} from './stripDays'
import { formatDegrees, WEATHER_CONDITIONS } from './weatherCondition'
import { useTripWeather } from './useTripWeather'

/** Same height while loading, with a forecast and without one (h-28 = 112px; DayTimeline counts on it) */
const BOX_HEIGHT = 'h-28'

interface WeatherStripProps {
  tripId: number
  /** The days of the trip, for the number each forecast belongs to */
  days: TripDay[]
  /** The day on screen: its cell is highlighted and its page is the one shown */
  currentDayIndex: number
}

/**
 * The forecast of the trip under the day map (UI_GUIDE 9): one cell per day that has a forecast, each a link to
 * its day. Days already over and days further ahead than the forecast reaches are left out; when that leaves
 * nothing, or the trip has no destination position, the strip says why in one line. More cells than fit are
 * split into pages turned with two buttons; the strip never scrolls sideways.
 * The box keeps one height whatever it holds: the map above it measures its own box only once.
 * <p>
 * The forecast is an extra: when it cannot be loaded the strip says so and the rest of the page works as usual.
 * A forecast loaded earlier stays on screen when a later reload fails (same rule as the trip itself, BUG-UI-005).
 */
export function WeatherStrip({ tripId, days, currentDayIndex }: WeatherStripProps) {
  const { data: weather, isPending, isFetching, refetch } = useTripWeather(tripId)
  const today = useToday()

  if (isPending) {
    return (
      <div role="status" aria-label="Đang tải dự báo thời tiết">
        <Skeleton className={`${BOX_HEIGHT} w-full rounded-card`} />
      </div>
    )
  }

  const dayIndexById = new Map(days.map((day) => [day.id, day.dayIndex]))
  const shown = weather?.status === 'OK' ? daysWithForecast(weather.days) : []
  // A day the trip no longer has is skipped (the trip was just shortened, the forecast is being reloaded)
  const cells = shown.flatMap((day) => {
    const dayIndex = dayIndexById.get(day.dayId)
    return dayIndex === undefined ? [] : [{ day, dayIndex }]
  })

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
      {cells.length > 0 && <ForecastPages tripId={tripId} cells={cells} currentDayIndex={currentDayIndex} />}
    </section>
  )
}

interface ForecastCell {
  day: ForecastDay
  dayIndex: number
}

interface ForecastPagesProps {
  tripId: number
  cells: ForecastCell[]
  currentDayIndex: number
}

/**
 * The cells, as many as the width holds at a time. When they do not all fit, a button at each end turns a
 * whole page; the page that holds the day on screen is the one shown whenever that day changes, and the user
 * is free to turn away from it. A page that is not full keeps the cell width of the others.
 */
function ForecastPages({ tripId, cells, currentDayIndex }: ForecastPagesProps) {
  const [box, setBox] = useState<HTMLDivElement | null>(null)
  // 0 until the box has been measured; nothing is drawn before that, so no layout is shown and then replaced
  const [width, setWidth] = useState(0)
  useEffect(() => {
    if (!box) return
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(box)
    return () => observer.disconnect()
  }, [box])

  const { size, paged } = stripLayout(width, cells.length)
  const pages = pageCount(cells.length, size)
  const currentPosition = cells.findIndex((cell) => cell.dayIndex === currentDayIndex)

  // The page follows the day on screen each time that day, or the way the cells are split, changes. Between
  // two such changes it belongs to the buttons. Adjusted while rendering, as React recommends for state that
  // follows a prop, instead of in an effect that would draw the old page first.
  const follows = `${currentDayIndex}|${size}|${cells.length}`
  const [view, setView] = useState({ follows: '', page: 0 })
  let page = view.page
  if (view.follows !== follows) {
    // A day without a cell (already over) leaves the page where it is
    page = currentPosition >= 0 ? pageOf(currentPosition, size) : view.page
    setView({ follows, page })
  }
  page = Math.min(page, pages - 1)

  const turn = (step: -1 | 1) => setView({ follows, page: page + step })

  return (
    <div ref={setBox} className="flex h-full">
      {width > 0 && (
        <>
          {paged && (
            <PageButton label="Các ngày trước" disabled={page === 0} onClick={() => turn(-1)} side="left">
              <ChevronLeft aria-hidden className="size-4" />
            </PageButton>
          )}
          <ol
            className="grid h-full min-w-0 flex-1 divide-x divide-tide"
            style={{ gridTemplateColumns: `repeat(${size}, minmax(0, 1fr))` }}
          >
            {cells.slice(page * size, (page + 1) * size).map(({ day, dayIndex }) => (
              <li key={day.dayId} className="min-w-0">
                <DayCell tripId={tripId} dayIndex={dayIndex} day={day} current={dayIndex === currentDayIndex} />
              </li>
            ))}
          </ol>
          {paged && (
            <PageButton label="Các ngày sau" disabled={page === pages - 1} onClick={() => turn(1)} side="right">
              <ChevronRight aria-hidden className="size-4" />
            </PageButton>
          )}
        </>
      )}
    </div>
  )
}

interface PageButtonProps {
  label: string
  disabled: boolean
  onClick: () => void
  side: 'left' | 'right'
  children: ReactNode
}

/** 32px wide, as tall as the strip: an easy target without taking the room of a cell. */
function PageButton({ label, disabled, onClick, side, children }: PageButtonProps) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className={`flex w-8 shrink-0 items-center justify-center border-tide text-gray-600 transition-colors hover:bg-gray-100 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none focus-visible:ring-inset disabled:cursor-not-allowed disabled:text-gray-300 disabled:hover:bg-transparent ${
        side === 'left' ? 'border-r' : 'border-l'
      }`}
    >
      {children}
    </button>
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
      className={`flex h-full flex-col items-center justify-center gap-0.5 px-1 text-center transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none focus-visible:ring-inset ${
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
