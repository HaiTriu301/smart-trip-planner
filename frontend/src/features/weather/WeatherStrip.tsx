import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import {
  CalendarClock,
  CalendarCheck,
  ChevronLeft,
  ChevronRight,
  CloudAlert,
  CloudSun,
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
  TILE_GAP,
  type ForecastDay,
} from './stripDays'
import { formatDegrees, WEATHER_CONDITIONS } from './weatherCondition'
import { useTripWeather } from './useTripWeather'

interface WeatherStripProps {
  tripId: number
  /** The days of the trip, for the number each forecast belongs to */
  days: TripDay[]
  /** The day on screen: its tile is highlighted and its page is the one shown */
  currentDayIndex: number
  /** Name of the trip's destination, shown in the card heading; null when the trip has none */
  destinationName: string | null
}

/**
 * The forecast of the trip, a card under the day map (UI_GUIDE 9): a heading, then one tile per day that has a
 * forecast, each a link to its day. Days already over and days further ahead than the forecast reaches are left
 * out; when that leaves nothing, or the trip has no destination position, the card says why in one line. More
 * tiles than fit are split into pages turned with two buttons in the heading; nothing scrolls sideways.
 * The card keeps one height whatever it holds: the map above it measures its own box only once.
 * <p>
 * The forecast is an extra: when it cannot be loaded the card says so and the rest of the page works as usual.
 * A forecast loaded earlier stays on screen when a later reload fails (same rule as the trip itself, BUG-UI-005).
 */
export function WeatherStrip({ tripId, days, currentDayIndex, destinationName }: WeatherStripProps) {
  const { data: weather, isPending, isFetching, refetch } = useTripWeather(tripId)
  const today = useToday()

  if (isPending) {
    return (
      <WeatherCard destinationName={destinationName}>
        <div role="status" aria-label="Đang tải dự báo thời tiết" className="h-full">
          <Skeleton className="h-full w-full" />
        </div>
      </WeatherCard>
    )
  }

  if (!weather) {
    return (
      <WeatherCard destinationName={destinationName}>
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
      </WeatherCard>
    )
  }

  if (weather.status === 'NO_DESTINATION') {
    return (
      <WeatherCard destinationName={destinationName}>
        {/* Same wording as the note on the map of a trip without a destination (UI_GUIDE 9) */}
        <Note icon={<MapPinOff aria-hidden className="size-5 shrink-0 text-gray-400" />}>
          Đặt vị trí điểm đến trong "Sửa" chuyến đi để xem dự báo thời tiết.
        </Note>
      </WeatherCard>
    )
  }

  const dayIndexById = new Map(days.map((day) => [day.id, day.dayIndex]))
  // A day the trip no longer has is skipped (the trip was just shortened, the forecast is being reloaded)
  const cells = daysWithForecast(weather.days).flatMap((day) => {
    const dayIndex = dayIndexById.get(day.dayId)
    return dayIndex === undefined ? [] : [{ day, dayIndex }]
  })

  if (cells.length === 0) {
    return (
      <WeatherCard destinationName={destinationName}>
        {noForecastReason(weather.days, today) === 'over' ? (
          <Note icon={<CalendarCheck aria-hidden className="size-5 shrink-0 text-gray-400" />}>
            Chuyến đi đã qua, không còn dự báo thời tiết.
          </Note>
        ) : (
          <Note icon={<CalendarClock aria-hidden className="size-5 shrink-0 text-gray-400" />}>
            Chưa có dự báo. Dự báo chỉ có cho 16 ngày tới, chuyến đi này bắt đầu sau đó.
          </Note>
        )}
      </WeatherCard>
    )
  }

  return (
    <ForecastPages tripId={tripId} cells={cells} currentDayIndex={currentDayIndex} destinationName={destinationName} />
  )
}

interface WeatherCardProps {
  destinationName: string | null
  /** Buttons at the right end of the heading: the two that turn the pages */
  actions?: ReactNode
  children: ReactNode
}

/**
 * The card around every state of the forecast: heading with the destination, then a body of a fixed height.
 * h-[212px] in all: DayTimeline gives the map above it what is left of the screen and counts on this number.
 */
function WeatherCard({ destinationName, actions, children }: WeatherCardProps) {
  return (
    <section aria-label="Dự báo thời tiết" className="flex h-[212px] flex-col rounded-card bg-white shadow-md">
      <div className="flex h-[52px] shrink-0 items-center justify-between gap-3 px-4">
        <p className="flex shrink-0 items-center gap-2 text-lg leading-6 font-semibold text-ink">
          <CloudSun aria-hidden className="size-5 shrink-0 text-sun" />
          Thời tiết dự báo
        </p>
        <div className="flex min-w-0 items-center gap-2">
          {destinationName && (
            <span title={destinationName} className="truncate text-xs text-gray-500">
              {destinationName}
            </span>
          )}
          {actions}
        </div>
      </div>
      <div className="min-h-0 flex-1 px-3 pb-3">{children}</div>
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
  destinationName: string | null
}

/**
 * The tiles, as many as the width holds at a time. When they do not all fit, the two buttons in the heading
 * turn a whole page; the page that holds the day on screen is the one shown whenever that day changes, and the
 * user is free to turn away from it. A page that is not full keeps the tile width of the others.
 */
function ForecastPages({ tripId, cells, currentDayIndex, destinationName }: ForecastPagesProps) {
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

  // The page follows the day on screen each time that day, or the way the tiles are split, changes. Between
  // two such changes it belongs to the buttons. Adjusted while rendering, as React recommends for state that
  // follows a prop, instead of in an effect that would draw the old page first.
  const follows = `${currentDayIndex}|${size}|${cells.length}`
  const [view, setView] = useState({ follows: '', page: 0 })
  let page = view.page
  if (view.follows !== follows) {
    // A day without a tile (already over) leaves the page where it is
    page = currentPosition >= 0 ? pageOf(currentPosition, size) : view.page
    setView({ follows, page })
  }
  page = Math.min(page, pages - 1)

  const turn = (step: -1 | 1) => setView({ follows, page: page + step })

  return (
    <WeatherCard
      destinationName={destinationName}
      actions={
        paged && (
          <div className="flex shrink-0 items-center gap-1">
            <PageButton label="Các ngày trước" disabled={page === 0} onClick={() => turn(-1)}>
              <ChevronLeft aria-hidden className="size-4" />
            </PageButton>
            <PageButton label="Các ngày sau" disabled={page === pages - 1} onClick={() => turn(1)}>
              <ChevronRight aria-hidden className="size-4" />
            </PageButton>
          </div>
        )
      }
    >
      <div ref={setBox} className="h-full">
        {width > 0 && (
          <ol
            className="grid h-full"
            style={{ gridTemplateColumns: `repeat(${size}, minmax(0, 1fr))`, columnGap: TILE_GAP }}
          >
            {cells.slice(page * size, (page + 1) * size).map(({ day, dayIndex }) => (
              <li key={day.dayId} className="min-w-0">
                <DayTile tripId={tripId} dayIndex={dayIndex} day={day} current={dayIndex === currentDayIndex} />
              </li>
            ))}
          </ol>
        )}
      </div>
    </WeatherCard>
  )
}

interface PageButtonProps {
  label: string
  disabled: boolean
  onClick: () => void
  children: ReactNode
}

/** 28px square in the card heading; 44px on touch screens. */
function PageButton({ label, disabled, onClick, children }: PageButtonProps) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      disabled={disabled}
      onClick={onClick}
      className="flex size-7 items-center justify-center rounded-control border border-tide text-gray-600 transition-colors hover:bg-gray-100 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none disabled:cursor-not-allowed disabled:text-gray-300 disabled:hover:bg-transparent pointer-coarse:size-11"
    >
      {children}
    </button>
  )
}

/** One line with an icon, for every state in which the card has no tiles to show. */
function Note({ icon, children }: { icon: ReactNode; children: ReactNode }) {
  return (
    <div className="flex h-full items-center gap-3 rounded-control bg-gray-50 px-4">
      {icon}
      <p className="text-sm text-gray-600">{children}</p>
    </div>
  )
}

interface DayTileProps {
  tripId: number
  dayIndex: number
  day: ForecastDay
  current: boolean
}

/** The weather of one day: the high in large figures, the low under it, then the chance of rain. */
function DayTile({ tripId, dayIndex, day, current }: DayTileProps) {
  const shortDate = formatDate(day.date).slice(0, 5)
  const forecast = day.forecast
  const look = WEATHER_CONDITIONS[forecast.condition]

  return (
    <Link
      to={`/trips/${tripId}/days/${dayIndex}`}
      aria-current={current ? 'page' : undefined}
      aria-label={`Ngày ${dayIndex}, ${shortDate}: ${look.label}, cao nhất ${formatDegrees(forecast.tempMax)}, thấp nhất ${formatDegrees(forecast.tempMin)}, khả năng mưa ${forecast.precipitationProbability}%`}
      className={`flex h-full flex-col items-center justify-center rounded-control px-1 text-center transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none ${
        current ? 'bg-jade-light' : 'bg-gray-100 hover:bg-gray-200'
      }`}
    >
      <span className={`tabular text-xs leading-4 ${current ? 'font-semibold text-jade-dark' : 'text-gray-600'}`}>
        N{dayIndex} · {shortDate}
      </span>
      <look.Icon aria-hidden className={`mt-1.5 size-6 ${look.color}`} />
      <span className="tabular mt-1 text-lg leading-6 font-bold text-ink">{formatDegrees(forecast.tempMax)}</span>
      <span className="tabular text-xs leading-4 text-gray-500">{formatDegrees(forecast.tempMin)}</span>
      <span className="tabular mt-1 inline-flex items-center gap-0.5 text-xs leading-4 text-gray-600">
        <Droplet aria-hidden className="size-3 text-info" />
        {forecast.precipitationProbability}%
      </span>
    </Link>
  )
}
