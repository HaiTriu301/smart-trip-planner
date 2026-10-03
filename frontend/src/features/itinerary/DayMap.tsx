import { lazy, Suspense } from 'react'
import { Skeleton } from '../../components/Skeleton'
import type { Activity, ActivityType } from '../../types/activity'

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
}

/**
 * The map of one day (UI_GUIDE 9): a marker for every activity that has a place. The frame is its own
 * stacking context: the map library stacks its layers up to z-index 1000, and without "isolate" they would
 * cover the sticky day header and the dialogs' backdrop (BUG-UI-002).
 */
export function DayMap({ activities }: DayMapProps) {
  return (
    <section
      aria-label="Bản đồ của ngày"
      className="isolate h-full overflow-hidden rounded-card border border-tide bg-gray-100"
    >
      <Suspense fallback={<Skeleton className="h-full" />}>
        <DayMapCanvas stops={toStops(activities)} />
      </Suspense>
    </section>
  )
}
