import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { getTrip } from '../api/trips'
import { getApiError, getErrorMessage } from '../api/errors'
import { Alert } from '../components/Alert'
import { DayTimeline } from '../features/itinerary/DayTimeline'
import { TripActions } from '../features/trips/TripActions'
import { TRIP_STATUS_LABELS, TRIP_STATUS_STYLES } from '../features/trips/tripStatus'
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
  if (isPending) return <p className="text-slate-500">Đang tải chuyến đi...</p>
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
      <Link to="/trips" className="text-sm text-sky-700 hover:underline">
        ← Chuyến đi của tôi
      </Link>

      <header className="space-y-2">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-bold text-slate-800">{trip.title}</h1>
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${TRIP_STATUS_STYLES[trip.status]}`}>
              {TRIP_STATUS_LABELS[trip.status]}
            </span>
          </div>
          <TripActions trip={trip} />
        </div>
        <p className="text-slate-600">
          {trip.destinationName && <>{trip.destinationName} · </>}
          {formatDateRange(trip.startDate, trip.endDate)} · {countDays(trip.startDate, trip.endDate)} ngày
          {trip.budgetAmount !== null && <> · Ngân sách {formatMoney(trip.budgetAmount, trip.currency)}</>}
        </p>
        {trip.description && <p className="whitespace-pre-line text-slate-600">{trip.description}</p>}
      </header>

      <DayTimeline tripId={trip.id} days={trip.days} />
    </div>
  )
}

function TripUnavailable({ message }: { message: string }) {
  return (
    <div className="space-y-4">
      <Alert variant="error">{message}</Alert>
      <Link to="/trips" className="text-sm text-sky-700 hover:underline">
        ← Về danh sách chuyến đi
      </Link>
    </div>
  )
}
