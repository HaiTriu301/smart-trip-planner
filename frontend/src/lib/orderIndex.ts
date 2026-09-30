// orderIndex for a dropped activity (design.md rule 14.5, 10.2 "Quy ước Reorder"). Indexes are spaced 1000
// apart, so a drop between two neighbours takes the midpoint and only the moved activity is sent. The
// backend renumbers a day whose gaps fall under 10, so after any reorder a midpoint normally exists.

export const ORDER_STEP = 1000
const MIN_INDEX = 1
const MAX_INDEX = 1_000_000_000

export interface ReorderItem {
  activityId: number
  dayId: number
  orderIndex: number
}

interface Ordered {
  id: number
  orderIndex: number
}

/**
 * Items for PUT /activities/reorder after `movedId` was dropped into `dayId`. `dayActivities` is the target day
 * in its new order, already containing the moved activity at the drop position; the others keep their indexes.
 * Normally one item; when no free integer is left between the neighbours, the whole day is renumbered.
 */
export function reorderItemsForDrop(dayId: number, dayActivities: readonly Ordered[], movedId: number): ReorderItem[] {
  const position = dayActivities.findIndex((a) => a.id === movedId)
  const prev = position > 0 ? dayActivities[position - 1].orderIndex : 0
  const next = position < dayActivities.length - 1 ? dayActivities[position + 1].orderIndex : null

  const candidate = next === null ? prev + ORDER_STEP : Math.floor((prev + next) / 2)
  const fits = candidate > prev && (next === null || candidate < next) && candidate >= MIN_INDEX && candidate <= MAX_INDEX
  if (fits) {
    return [{ activityId: movedId, dayId, orderIndex: candidate }]
  }
  return dayActivities.map((a, i) => ({ activityId: a.id, dayId, orderIndex: (i + 1) * ORDER_STEP }))
}
