import type { Activity } from '../../types/activity'
import type { TripDayDetail } from '../../types/trip'

/**
 * The activity as the days just loaded from the server have it, in whatever day it sits now (somebody may have
 * moved it); undefined when it is not there any more. Read after a save refused with 409 STALE_VERSION: the
 * form takes the new version from it, or learns that the activity was deleted (design.md 11.5).
 */
export function findActivity(days: TripDayDetail[], activityId: number): Activity | undefined {
  for (const day of days) {
    const found = day.activities.find((activity) => activity.id === activityId)
    if (found) return found
  }
  return undefined
}
