import type { RouteLeg } from '../types/route'

interface Stop {
  id: number
  place: object | null
}

/**
 * The leg to draw under each activity, keyed by the activity the leg leaves from. `activities` is the day as it
 * is on screen, which may be ahead of the legs: a card was just dragged, a place was just removed. A leg is kept
 * only while it still describes what is shown, so an outdated number is never drawn at the wrong spot:
 * its two activities are next to each other, in that order, and both have a place. A leg of 0 metres (two
 * activities at the same place) says nothing and is left out.
 */
export function legsAfter(activities: readonly Stop[], legs: readonly RouteLeg[]): Map<number, RouteLeg> {
  const position = new Map(activities.map((activity, index) => [activity.id, index]))
  const shown = new Map<number, RouteLeg>()
  for (const leg of legs) {
    const from = position.get(leg.fromActivityId)
    const to = position.get(leg.toActivityId)
    if (from === undefined || to === undefined || to !== from + 1) continue
    if (!activities[from].place || !activities[to].place) continue
    if (leg.distanceMeters <= 0) continue
    shown.set(leg.fromActivityId, leg)
  }
  return shown
}
