import { TripList } from '../features/trips/TripList'

export function TripsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Chuyến đi của tôi</h1>
      <TripList />
    </div>
  )
}
