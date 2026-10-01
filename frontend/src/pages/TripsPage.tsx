import { Plus } from 'lucide-react'
import { LinkButton } from '../components/LinkButton'
import { TripList } from '../features/trips/TripList'

export function TripsPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-[32px] leading-10 font-bold tracking-[-0.02em] text-ink">Chuyến đi của bạn</h1>
          <p className="mt-1 text-gray-600">Quản lý lịch trình và các điểm dừng của từng chuyến đi.</p>
        </div>
        <LinkButton to="/trips/new" className="shrink-0">
          <Plus aria-hidden className="size-4" />
          Tạo chuyến đi
        </LinkButton>
      </div>
      <TripList />
    </div>
  )
}
