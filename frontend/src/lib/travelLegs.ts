import type { RouteLeg } from '../types/route'

interface Stop {
  id: number
  place: object | null
}

/** A leg that is drawn, under the activity it leaves from. */
export interface ShownLeg {
  leg: RouteLeg
  /** False when activities without a place sit between its two ends: the next card is not where it arrives */
  direct: boolean
}

/**
 * The leg to draw under each activity, keyed by the activity the leg leaves from. `activities` is the day as it
 * is on screen, which may be ahead of the legs: a card was just dragged, a place was just removed. A leg is kept
 * only while it still describes what is shown, so an outdated number is never drawn at the wrong spot:
 * it goes down the list, both its ends have a place, and nothing between them has one (the server skips
 * activities without a place, design.md 10.2 "Quy ước Route": A → B without a place → C is one leg A → C).
 * A leg of 0 metres (two activities at the same place) says nothing and is left out.
 */
export function legsAfter(activities: readonly Stop[], legs: readonly RouteLeg[]): Map<number, ShownLeg> {
  const position = new Map(activities.map((activity, index) => [activity.id, index]))
  const shown = new Map<number, ShownLeg>()
  for (const leg of legs) {
    const from = position.get(leg.fromActivityId)
    const to = position.get(leg.toActivityId)
    if (from === undefined || to === undefined || to <= from) continue
    if (!activities[from].place || !activities[to].place) continue
    // A place in between means the day now has a stop this leg does not know about
    if (activities.slice(from + 1, to).some((activity) => activity.place)) continue
    if (leg.distanceMeters <= 0) continue
    shown.set(leg.fromActivityId, { leg, direct: to === from + 1 })
  }
  return shown
}
