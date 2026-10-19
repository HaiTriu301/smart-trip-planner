import { ACTIVITY_ROUTE } from '../itinerary/activityType'
import type { Activity } from '../../types/activity'

/**
 * The activity a comment is about (UI_GUIDE 8.1): a small gray pill with an 8px dot in the route colour of its type
 * (UI_GUIDE 3.4) and its title, cut with "…" when long. The colour only decorates: the title says which activity.
 */
export function ActivityChip({ activity }: { activity: Pick<Activity, 'title' | 'type'> }) {
  return (
    <span
      title={activity.title}
      className="inline-flex h-5 max-w-full min-w-0 items-center gap-1.5 rounded-full bg-gray-100 px-2 text-xs leading-4 text-gray-700"
    >
      <span aria-hidden className={`size-2 shrink-0 rounded-full ${ACTIVITY_ROUTE[activity.type].marker}`} />
      <span className="sr-only">Về hoạt động: </span>
      <span className="truncate">{activity.title}</span>
    </span>
  )
}
