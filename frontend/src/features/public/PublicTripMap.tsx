import { lazy, Suspense } from 'react'
import { Map as MapIcon } from 'lucide-react'
import { Skeleton } from '../../components/Skeleton'
import type { Coordinates } from '../../types/place'
import type { PublicDay } from '../../types/publicTrip'
import { publicStops } from './publicStops'

// The map library is large: fetched only when a public trip with places is shown
const DayMapCanvas = lazy(() => import('../itinerary/DayMapCanvas'))

interface PublicTripMapProps {
  days: PublicDay[]
  destination: Coordinates | null
}

/**
 * One map with every place of the trip (UI_GUIDE 8.5, Task 4.5 decision 2), under the summary of the public page,
 * not sticky: a card like the day map of the app, with a 320px map that frames all the places. Markers in the
 * colour of the activity type, without numbers or a line, since they belong to different days. Nothing at all
 * when no activity has a place.
 */
export function PublicTripMap({ days, destination }: PublicTripMapProps) {
  const stops = publicStops(days)
  if (stops.length === 0) return null
  return (
    <section aria-label="Bản đồ chuyến đi" className="overflow-hidden rounded-card bg-white shadow-md">
      <div className="flex items-center justify-between gap-3 px-4 py-3">
        <p className="flex min-w-0 items-center gap-2 text-lg leading-6 font-semibold text-ink">
          <MapIcon aria-hidden className="size-5 shrink-0 text-gray-500" />
          Bản đồ chuyến đi
        </p>
        <span className="tabular inline-flex h-6 shrink-0 items-center rounded-control bg-jade-light px-2 text-xs font-medium text-jade-dark">
          {stops.length} địa điểm
        </span>
      </div>
      {/* Its own stacking context, and the map pinned to the four edges of a fixed box: the map library stacks
          its layers up to z-index 1000 and measures its box only once (BUG-UI-002, Task 3.9) */}
      <div className="relative isolate h-80">
        <div className="absolute inset-0 z-0">
          <Suspense fallback={<Skeleton className="h-full" />}>
            <DayMapCanvas stops={stops} destination={destination} overview />
          </Suspense>
        </div>
      </div>
    </section>
  )
}
