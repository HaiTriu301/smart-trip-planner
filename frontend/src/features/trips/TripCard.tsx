import { Link } from 'react-router-dom'
import { CalendarDays, Clock, MapPin } from 'lucide-react'
import { Badge } from '../../components/Badge'
import { Skeleton } from '../../components/Skeleton'
import { useToday } from '../../hooks/useToday'
import { countDays, formatDateRange } from '../../lib/format'
import type { TripSummary } from '../../types/trip'
import { cardForecastTarget } from '../weather/cardForecast'
import { TripCardWeather } from '../weather/TripCardWeather'
import { TRIP_STATUS_LABELS, TRIP_STATUS_TONES } from './tripStatus'

/**
 * One trip in the grid (UI_GUIDE 8.2, Stitch mockup): 16:9 cover with the status badge top-right and the
 * destination bottom-left, then title, dates and length. No gradient over the photo: the labels have
 * their own solid backgrounds. The footer also carries the weather of a trip in progress or about to start;
 * only those cards ask the server for a forecast.
 */
export function TripCard({ trip }: { trip: TripSummary }) {
  const forecastTarget = cardForecastTarget(trip, useToday())
  return (
    <Link
      to={`/trips/${trip.id}`}
      className="group flex flex-col overflow-hidden rounded-card border border-tide bg-white transition-colors hover:border-jade focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
    >
      <div className="relative aspect-video overflow-hidden bg-gray-100">
        {trip.coverImageUrl ? (
          <img src={trip.coverImageUrl} alt="" className="size-full object-cover" />
        ) : (
          <div className="flex size-full items-center justify-center text-gray-300" aria-hidden>
            <MapPin className="size-10" strokeWidth={1.5} />
          </div>
        )}
        <div className="absolute top-3 right-3">
          <Badge tone={TRIP_STATUS_TONES[trip.status]} surface="solid">
            {TRIP_STATUS_LABELS[trip.status]}
          </Badge>
        </div>
        {trip.destinationName && (
          <span className="absolute bottom-3 left-3 inline-flex max-w-[calc(100%-1.5rem)] items-center gap-1 rounded-control bg-ink/85 px-2 py-0.5 text-[11px] leading-4 font-medium text-white">
            <MapPin aria-hidden className="size-3 shrink-0" />
            <span className="truncate">{trip.destinationName}</span>
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-1.5 p-5">
        <h2
          title={trip.title}
          className="line-clamp-1 text-lg leading-[26px] font-semibold wrap-anywhere text-ink transition-colors group-hover:text-jade"
        >
          {trip.title}
        </h2>
        <p className="tabular flex items-center gap-1.5 text-sm text-gray-600">
          <CalendarDays aria-hidden className="size-4 shrink-0" />
          {formatDateRange(trip.startDate, trip.endDate)}
        </p>
        {/* Short of room (three narrow columns), the weather goes to a second line, still on the right */}
        <p className="mt-auto flex flex-wrap items-center gap-x-3 gap-y-1 border-t border-tide pt-3 text-[13px] text-gray-600">
          <span className="inline-flex items-center gap-1.5">
            <Clock aria-hidden className="size-3.5 shrink-0" />
            {countDays(trip.startDate, trip.endDate)} ngày · {trip.activityCount} hoạt động
          </span>
          {forecastTarget && <TripCardWeather tripId={trip.id} target={forecastTarget} />}
        </p>
      </div>
    </Link>
  )
}

/** Same outline as a card while the list loads (UI_GUIDE 7.8). */
export function TripCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-card border border-tide bg-white">
      <Skeleton className="aspect-video w-full" />
      <div className="space-y-3 p-5">
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="mt-4 h-4 w-1/4" />
      </div>
    </div>
  )
}
