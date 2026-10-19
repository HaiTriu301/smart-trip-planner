import { ACTIVITY_ROUTE } from '../itinerary/activityType'
import type { Activity } from '../../types/activity'

const PILL = 'inline-flex h-5 max-w-full min-w-0 items-center gap-1.5 rounded-full bg-gray-100 px-2 text-xs leading-4 text-gray-700'

interface ActivityChipProps {
  activity: Pick<Activity, 'title' | 'type'>
  /** Present: the chip is a button that shows the activity in the itinerary (Task 4.6 Mốc 10) */
  onOpen?: () => void
}

/**
 * The activity a comment is about (UI_GUIDE 8.1): a small gray pill with an 8px dot in the route colour of its type
 * (UI_GUIDE 3.4) and its title, cut with "…" when long. The colour only decorates: the title says which activity.
 * With onOpen it is a button: the comment panel closes and the card of the activity shows, on its own day.
 */
export function ActivityChip({ activity, onOpen }: ActivityChipProps) {
  const content = (
    <>
      <span aria-hidden className={`size-2 shrink-0 rounded-full ${ACTIVITY_ROUTE[activity.type].marker}`} />
      <span className="truncate">{activity.title}</span>
    </>
  )
  if (!onOpen) {
    return (
      <span title={activity.title} className={PILL}>
        <span className="sr-only">Về hoạt động: </span>
        {content}
      </span>
    )
  }
  return (
    <button
      type="button"
      onClick={onOpen}
      title={activity.title}
      aria-label={`Xem ${activity.title} trong lịch trình`}
      // 32px tall on touch screens: a little pill cannot reach 44px without looking like a button bar
      className={`${PILL} transition-colors hover:bg-gray-200 hover:text-ink focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none pointer-coarse:h-8`}
    >
      {content}
    </button>
  )
}
