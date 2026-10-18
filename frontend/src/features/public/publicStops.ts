import type { DayStop } from '../itinerary/DayMap'
import type { PublicDay } from '../../types/publicTrip'

/**
 * Every place of a public trip, for the one map of the page (Task 4.5 decision 2): the activities that have a
 * place, day after day, in the order the owner arranged them. The server sends no ids, so the position in the
 * whole trip stands in for one (it only has to be unique on this map). The name says which day it belongs to,
 * since the map has no numbers and no line: "Ngày 2 · Bún chả cá 109".
 */
export function publicStops(days: PublicDay[]): DayStop[] {
  const stops: DayStop[] = []
  for (const day of days) {
    for (const activity of day.activities) {
      if (!activity.place) continue
      stops.push({
        activityId: stops.length + 1,
        number: stops.length + 1,
        title: `Ngày ${day.dayIndex} · ${activity.title}`,
        type: activity.type,
        lat: activity.place.lat,
        lng: activity.place.lng,
      })
    }
  }
  return stops
}
