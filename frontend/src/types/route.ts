// Mirrors backend dto/response/DayRouteResponse (design.md 10.2 "Quy ước Route").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

/** Travel from one activity that has a place to the next one that has a place. An estimate, one vehicle. */
export interface RouteLeg {
  fromActivityId: number
  toActivityId: number
  /** Whole metres; 0 when both activities are at the same place */
  distanceMeters: number
  /** Whole seconds */
  durationSeconds: number
}

/** GET /trips/{tripId}/days/{dayId}/route: no leg at all for a day with fewer than two places. */
export interface DayRoute {
  legs: RouteLeg[]
  totalDistanceMeters: number
  totalDurationSeconds: number
}
