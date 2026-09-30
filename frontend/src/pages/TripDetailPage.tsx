import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { CalendarDays, ChevronLeft, Clock, MapPin, Wallet } from 'lucide-react'
import { getTrip } from '../api/trips'
import { getApiError, getErrorMessage } from '../api/errors'
import { Alert } from '../components/Alert'
import { ExpandableText } from '../components/ExpandableText'
import { Skeleton } from '../components/Skeleton'
import { DayTimeline } from '../features/itinerary/DayTimeline'
import { TripActions } from '../features/trips/TripActions'
import { TripStatusSelect } from '../features/trips/TripStatusSelect'
import { countDays, formatDateRange, formatMoney } from '../lib/format'

/** design.md 15 "màn hình chính": trip header + day timeline. GET /trips/{id} returns everything at once. */
export function TripDetailPage() {
  const { id } = useParams()
  const tripId = Number(id)
  const isValidId = Number.isInteger(tripId) && tripId > 0

  const { data: trip, error, isPending } = useQuery({
    queryKey: ['trip', tripId],
    queryFn: () => getTrip(tripId),
    enabled: isValidId,
  })

  if (!isValidId) return <TripUnavailable message="Không tìm thấy chuyến đi." />
  if (isPending) return <TripDetailSkeleton />
  if (error) {
    const code = getApiError(error)?.errorCode
    return (
      <TripUnavailable
        message={
          code === 'RESOURCE_NOT_FOUND'
            ? 'Không tìm thấy chuyến đi. Có thể chuyến đi đã bị xoá.'
            : code === 'FORBIDDEN'
              ? 'Bạn không có quyền xem chuyến đi này.'
              : getErrorMessage(error)
        }
      />
    )
  }

  return (
    <div className="space-y-6">
      <BackLink />

      <header className="space-y-3">
        {/* Title takes the free width and stops at two lines; status and actions keep one fixed spot on the
            right (on their own row under the title on narrow screens), whatever the title length */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <h1
            title={trip.title}
            className="line-clamp-2 min-w-0 flex-1 text-[32px] leading-10 font-bold tracking-[-0.02em] wrap-anywhere text-ink"
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

      <DayTimeline tripId={trip.id} days={trip.days} tripCurrency={trip.currency} />
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

function TripUnavailable({ message }: { message: string }) {
  return (
    <div className="space-y-4">
      <Alert variant="error">{message}</Alert>
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
