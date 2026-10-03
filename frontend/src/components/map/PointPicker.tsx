import { lazy, Suspense } from 'react'
import type { Coordinates } from '../../types/place'
import { Skeleton } from '../Skeleton'

// The map library is fetched when a picker is first shown, not with the app
const PointPickerCanvas = lazy(() => import('./PointPickerCanvas'))

interface PointPickerProps {
  /** The chosen point; null: none yet */
  value: Coordinates | null
  /** Where the map opens while no point is chosen; null: the whole country */
  around: Coordinates | null
  onPick: (point: Coordinates) => void
  /** Height of the map, e.g. "h-60" */
  heightClass: string
  /** What the map is for, read by screen readers */
  label: string
}

/**
 * A small map on which a click sets one point, shown as a jade teardrop (UI_GUIDE 9). The frame is its own
 * stacking context, like the map of a day: the layers of the map library must not cover a dialog around it.
 * A click needs a mouse or a finger; the forms that use the picker also offer the place search, which the
 * keyboard can drive.
 */
export function PointPicker({ value, around, onPick, heightClass, label }: PointPickerProps) {
  return (
    <section
      aria-label={label}
      className={`isolate overflow-hidden rounded-control border border-tide bg-gray-100 ${heightClass}`}
    >
      <Suspense fallback={<Skeleton className="h-full" />}>
        <PointPickerCanvas value={value} around={around} onPick={onPick} />
      </Suspense>
    </section>
  )
}
