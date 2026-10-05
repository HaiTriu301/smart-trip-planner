import { Route } from 'lucide-react'
import { formatDistance, formatDuration } from '../../lib/format'
import type { RouteLeg } from '../../types/route'

interface TravelLegProps {
  leg: RouteLeg
  /**
   * Title of the activity the leg arrives at, given only when that is not the next card: activities without a
   * place sit in between, and the numbers would otherwise read as the way to the card right below
   */
  destination?: string
}

/**
 * Travel from the card above to the next activity that has a place (UI_GUIDE 8.1): a pill centred between the
 * two cards, and the stretch of timeline next to it drawn dashed. The icon is a route, not a vehicle: the
 * server estimates for one vehicle only and does not say which.
 * <p>
 * It sits in the card column of its row, which starts 36px from the left of the list; the dashed stretch is
 * pulled back onto the line (centred 12px from the left) and covers the gaps above and below the pill. A
 * paper-coloured strip under it hides the solid line.
 */
export function TravelLeg({ leg, destination }: TravelLegProps) {
  return (
    <div className="relative mt-4 flex justify-center">
      <span aria-hidden className="absolute -top-4 -bottom-4 -left-8 flex w-4 justify-center">
        <span className="w-1.5 bg-paper" />
        <span className="absolute inset-y-0 border-l-2 border-dashed border-gray-300" />
      </span>
      <p
        title="Ước tính theo đường bộ, chưa tính kẹt xe"
        className="tabular inline-flex h-7 max-w-full items-center gap-1.5 rounded-full bg-gray-100 px-3 text-xs leading-none text-gray-600"
      >
        <Route aria-hidden className="size-3.5 shrink-0 text-gray-500" />
        <span className="sr-only">
          {destination ? 'Di chuyển, ước tính:' : 'Di chuyển tới hoạt động kế tiếp, ước tính:'}
        </span>
        <span className="shrink-0 font-semibold text-ink">{formatDuration(leg.durationSeconds)}</span>
        <span aria-hidden className="text-gray-400">
          ·
        </span>
        <span className="shrink-0">{formatDistance(leg.distanceMeters)}</span>
        {/* A long title is cut with "…"; the numbers before it always stay whole */}
        {destination && <span className="truncate">tới {destination}</span>}
      </p>
    </div>
  )
}
