import { formatDistance, formatDuration } from '../../lib/format'
import type { RouteLeg } from '../../types/route'

/**
 * Travel from the card above to the next activity (UI_GUIDE 8.1): one light line of text, and the stretch of
 * rail next to it drawn dashed. No vehicle icon: the server estimates for one vehicle only.
 * <p>
 * It sits in the card column of its row, 8px right of the rail; the dashed stretch is pulled back onto the
 * rail and runs through the gap to the next row. A paper-coloured strip under it hides the solid line.
 */
export function TravelLeg({ leg }: { leg: RouteLeg }) {
  const text = `${formatDuration(leg.durationSeconds)} · ${formatDistance(leg.distanceMeters)}`
  return (
    <p
      title="Ước tính theo đường bộ, chưa tính kẹt xe"
      className="tabular relative mt-1 pl-1 text-xs leading-5 text-gray-500"
    >
      <span aria-hidden className="absolute top-0 -bottom-3 -left-4 flex w-4 justify-center">
        <span className="w-1 bg-paper" />
        <span className="absolute inset-y-0 border-l-2 border-dashed border-gray-300" />
      </span>
      <span className="sr-only">Di chuyển tới hoạt động kế tiếp, ước tính: </span>
      {text}
    </p>
  )
}
