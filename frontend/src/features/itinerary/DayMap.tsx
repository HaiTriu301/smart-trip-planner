import { lazy, Suspense } from 'react'
import { Map as MapIcon } from 'lucide-react'
import { Skeleton } from '../../components/Skeleton'
import type { Activity, ActivityType } from '../../types/activity'
import type { Coordinates } from '../../types/place'

// The map library is large and only this page needs it: it is fetched when a trip is opened, not with the app
const DayMapCanvas = lazy(() => import('./DayMapCanvas'))

/** One marker of the day: an activity that has a place. */
export interface DayStop {
  activityId: number
  /** 1, 2, 3... among the activities that have a place, in the order the day shows them */
  number: number
  title: string
  type: ActivityType
  lat: number
  lng: number
}

/** Activities without a place are not on the map and take no number. */
function toStops(activities: Activity[]): DayStop[] {
  return activities.flatMap((activity) => (activity.place ? [{ activity, place: activity.place }] : []))
    .map(({ activity, place }, index) => ({
      activityId: activity.id,
      number: index + 1,
      title: activity.title,
      type: activity.type,
      lat: place.lat,
      lng: place.lng,
    }))
}

interface DayMapProps {
  /** Activities of the day being shown, in display order */
  activities: Activity[]
  /** Position of the trip's destination; null when the trip has none */
  destination: Coordinates | null
}

/**
 * The map of one day (UI_GUIDE 9): a marker for every activity that has a place. A day without any place
 * shows the area of the trip's destination and a note saying how to get markers. The frame is its own
 * stacking context: the map library stacks its layers up to z-index 1000, and without "isolate" they would
 * cover the sticky day header and the dialogs' backdrop (BUG-UI-002).
 */
export function DayMap({ activities, destination }: DayMapProps) {
  const stops = toStops(activities)
  return (
    <section
      aria-label="Bản đồ của ngày"
      className="relative isolate h-full overflow-hidden rounded-card border border-tide bg-gray-100"
    >
      {/* z-0 closes the layers of the map library into their own stack, so the note below can sit above them */}
      <div className="relative z-0 h-full">
        <Suspense fallback={<Skeleton className="h-full" />}>
          <DayMapCanvas stops={stops} destination={destination} />
        </Suspense>
      </div>
      {stops.length === 0 && (
        // The wrapper lets the pointer through: around the note the map can still be dragged and zoomed
        <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center p-6">
          <div className="pointer-events-auto max-w-[280px] rounded-card border border-tide bg-white px-5 py-4 text-center shadow-md">
            <MapIcon aria-hidden className="mx-auto size-5 text-gray-400" />
            <p className="mt-2 text-[15px] leading-5 font-semibold text-ink">Ngày này chưa có địa điểm nào.</p>
            <p className="mt-1 text-[13px] leading-5 text-gray-600">
              Thêm địa điểm cho hoạt động để thấy trên bản đồ.
              {!destination && ' Đặt vị trí điểm đến trong "Sửa" chuyến đi để bản đồ mở đúng nơi bạn đến.'}
            </p>
          </div>
        </div>
      )}
    </section>
  )
}
