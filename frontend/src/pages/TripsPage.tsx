import { Link } from 'react-router-dom'
import { TripList } from '../features/trips/TripList'

export function TripsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-800">Chuyến đi của tôi</h1>
        <Link
          to="/trips/new"
          className="rounded-control bg-jade px-4 py-2 font-medium text-white transition-colors hover:bg-jade-dark"
        >
          Tạo chuyến đi
        </Link>
      </div>
      <TripList />
    </div>
  )
}
