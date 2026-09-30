import { Link } from 'react-router-dom'
import { countDays, formatDateRange } from '../../lib/format'
import type { TripSummary } from '../../types/trip'
import { TRIP_STATUS_LABELS, TRIP_STATUS_STYLES } from './tripStatus'

export function TripCard({ trip }: { trip: TripSummary }) {
  return (
    <Link
      to={`/trips/${trip.id}`}
      className="block overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm transition-shadow hover:shadow-md focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-400"
    >
      {trip.coverImageUrl ? (
        <img src={trip.coverImageUrl} alt="" className="h-36 w-full object-cover" />
      ) : (
        <div className="h-36 w-full bg-linear-to-br from-sky-400 to-emerald-400" aria-hidden />
      )}
      <div className="space-y-2 p-4">
        <div className="flex items-start justify-between gap-2">
          <h2 className="line-clamp-2 font-semibold text-slate-800">{trip.title}</h2>
          <span
            className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${TRIP_STATUS_STYLES[trip.status]}`}
          >
            {TRIP_STATUS_LABELS[trip.status]}
          </span>
        </div>
        {trip.destinationName && <p className="text-sm text-slate-600">{trip.destinationName}</p>}
        <p className="text-sm text-slate-500">
          {formatDateRange(trip.startDate, trip.endDate)} · {countDays(trip.startDate, trip.endDate)} ngày
        </p>
      </div>
    </Link>
  )
}
