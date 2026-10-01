import { useQuery } from '@tanstack/react-query'
import { Link, Navigate, useParams } from 'react-router-dom'
import { CalendarDays, ChevronLeft, Clock, MapPin, Wallet } from 'lucide-react'
import { getTrip } from '../api/trips'
import { getApiError, getErrorMessage } from '../api/errors'
import { Alert } from '../components/Alert'
import { Button } from '../components/Button'
import { ExpandableText } from '../components/ExpandableText'
import { Skeleton } from '../components/Skeleton'
import { DayTimeline } from '../features/itinerary/DayTimeline'
import { TripActions } from '../features/trips/TripActions'
import { TripStatusSelect } from '../features/trips/TripStatusSelect'
import { countDays, formatDateRange, formatMoney } from '../lib/format'

/**
 * design.md 15 "màn hình chính": trip header + one day of the itinerary (/trips/:id/days/:dayIndex).
 * GET /trips/{id} returns every day at once; /trips/:id and an unknown day number go to day 1.
 */
export function TripDetailPage() {
  const { id, dayIndex } = useParams()
  const tripId = Number(id)
  const isValidId = Number.isInteger(tripId) && tripId > 0

  const { data: trip, error, isPending, isFetching, refetch } = useQuery({
    queryKey: ['trip', tripId],
    queryFn: () => getTrip(tripId),
    enabled: isValidId,
  })

  if (!isValidId) return <TripUnavailable message="Không tìm thấy chuyến đi." />
  if (isPending) return <TripDetailSkeleton />

  // The trip is gone or no longer ours: a copy loaded earlier must not stay on screen either
  const code = getApiError(error)?.errorCode
  if (code === 'RESOURCE_NOT_FOUND') {
    return <TripUnavailable message="Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá." />
  }
  if (code === 'FORBIDDEN') return <TripUnavailable message="Bạn không có quyền xem chuyến đi này." />

  const retry = { isRetrying: isFetching, onRetry: () => refetch() }
  // Nothing loaded yet and the first request failed (server down, network): only the error can be shown
  if (!trip) return <TripUnavailable message={getErrorMessage(error)} {...retry} />

  const currentDayIndex = Number(dayIndex)
  if (!trip.days.some((d) => d.dayIndex === currentDayIndex)) {
    return <Navigate to={`/trips/${trip.id}/days/1`} replace />
  }

  return (
    <div className="space-y-6">
      <BackLink />

      {/* A background reload failed (refetch on window focus while the server restarts...): keep the page and
          whatever the user is doing on it, with the trip as loaded earlier */}
      {error && (
        <RetryableError message={`${getErrorMessage(error)}. Đang hiển thị dữ liệu đã tải trước đó.`} {...retry} />
      )}

      <header className="space-y-3">
        {/* Title takes the free width and stops at two lines; status and actions keep one fixed spot on the
            right (on their own row under the title on narrow screens), whatever the title length */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          {/* data-page-top: "Lên đầu trang" puts the focus back here */}
          <h1
            title={trip.title}
            data-page-top
            tabIndex={-1}
            className="line-clamp-2 min-w-0 flex-1 text-[32px] leading-10 font-bold tracking-[-0.02em] wrap-anywhere text-ink focus:outline-none"
          >
            {trip.title}
          </h1>
          <div className="flex shrink-0 items-center justify-end gap-2 sm:pt-1">
            <TripStatusSelect trip={trip} />
            <TripActions trip={trip} />
          </div>
        </div>
        <ul className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-gray-600">
          {trip.destinationName && (
            <li className="inline-flex min-w-0 items-center gap-1.5 wrap-anywhere">
              <MapPin aria-hidden className="size-4 shrink-0 text-gray-400" />
              {trip.destinationName}
            </li>
          )}
          <li className="tabular inline-flex items-center gap-1.5">
            <CalendarDays aria-hidden className="size-4 shrink-0 text-gray-400" />
            {formatDateRange(trip.startDate, trip.endDate)}
          </li>
          <li className="inline-flex items-center gap-1.5">
            <Clock aria-hidden className="size-4 shrink-0 text-gray-400" />
            {countDays(trip.startDate, trip.endDate)} ngày
          </li>
          {trip.budgetAmount !== null && (
            <li className="tabular inline-flex items-center gap-1.5">
              <Wallet aria-hidden className="size-4 shrink-0 text-gray-400" />
              Ngân sách {formatMoney(trip.budgetAmount, trip.currency)}
            </li>
          )}
        </ul>
        {trip.description && <ExpandableText text={trip.description} className="max-w-[68ch] text-gray-600" />}
      </header>

      <DayTimeline tripId={trip.id} days={trip.days} currentDayIndex={currentDayIndex} tripCurrency={trip.currency} />
    </div>
  )
}

function BackLink() {
  return (
    <Link to="/trips" className="inline-flex items-center gap-1 text-sm font-medium text-jade hover:underline">
      <ChevronLeft aria-hidden className="size-4" />
      Chuyến đi của bạn
    </Link>
  )
}

interface RetryProps {
  isRetrying: boolean
  onRetry: () => void
}

/** Same pairing as the trip list: the error, then a "Thử lại" button under it. */
function RetryableError({ message, isRetrying, onRetry }: { message: string } & RetryProps) {
  return (
    <div className="space-y-3">
      <Alert variant="error">{message}</Alert>
      <Button variant="secondary" fullWidth={false} isLoading={isRetrying} onClick={onRetry}>
        Thử lại
      </Button>
    </div>
  )
}

/** The page cannot be shown at all. Retry is offered only when trying again can help (not for 404 / 403). */
function TripUnavailable({ message, isRetrying = false, onRetry }: { message: string } & Partial<RetryProps>) {
  return (
    <div className="space-y-4">
      {onRetry ? (
        <RetryableError message={message} isRetrying={isRetrying} onRetry={onRetry} />
      ) : (
        <Alert variant="error">{message}</Alert>
      )}
      <BackLink />
    </div>
  )
}

/** Outline of the header and four stations while the trip loads (UI_GUIDE 7.8: 4 activities). */
function TripDetailSkeleton() {
  return (
    <div className="space-y-6" role="status" aria-label="Đang tải chuyến đi">
      <Skeleton className="h-5 w-40" />
      <div className="space-y-3">
        <Skeleton className="h-10 w-2/3" />
        <Skeleton className="h-5 w-1/2" />
      </div>
      <div className="space-y-3 lg:pl-[232px]">
        <Skeleton className="h-6 w-64" />
        {Array.from({ length: 4 }, (_, i) => (
          <Skeleton key={i} className="ml-16 h-20" />
        ))}
      </div>
    </div>
  )
}
