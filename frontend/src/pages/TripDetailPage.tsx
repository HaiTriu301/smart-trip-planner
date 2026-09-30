import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getTrip } from '../api/trips'
import { getApiError, getErrorMessage } from '../api/errors'
import { Alert } from '../components/Alert'
import { ExpandableText } from '../components/ExpandableText'
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
  if (isPending) return <p className="text-gray-500">Đang tải chuyến đi...</p>
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
      <Link to="/trips" className="text-sm text-jade-dark hover:underline">
        ← Chuyến đi của tôi
      </Link>

      <header className="space-y-2">
        {/* Title takes the free width and stops at two lines; status and actions keep one fixed spot on the
            right (on their own row under the title on narrow screens), whatever the title length */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <h1 title={trip.title} className="line-clamp-2 min-w-0 flex-1 text-2xl font-bold wrap-anywhere text-gray-800">
            {trip.title}
          </h1>
          <div className="flex shrink-0 items-center justify-end gap-2 sm:pt-1">
            <TripStatusSelect trip={trip} />
            <TripActions trip={trip} />
          </div>
        </div>
        <p className="wrap-anywhere text-gray-600">
          {trip.destinationName && <>{trip.destinationName} · </>}
          {formatDateRange(trip.startDate, trip.endDate)} · {countDays(trip.startDate, trip.endDate)} ngày
          {trip.budgetAmount !== null && <> · Ngân sách {formatMoney(trip.budgetAmount, trip.currency)}</>}
        </p>
        {trip.description && <ExpandableText text={trip.description} className="text-gray-600" />}
      </header>

      <DayTimeline tripId={trip.id} days={trip.days} tripCurrency={trip.currency} />
    </div>
  )
}

function TripUnavailable({ message }: { message: string }) {
  return (
    <div className="space-y-4">
      <Alert variant="error">{message}</Alert>
      <Link to="/trips" className="text-sm text-jade-dark hover:underline">
        ← Về danh sách chuyến đi
      </Link>
    </div>
  )
}
