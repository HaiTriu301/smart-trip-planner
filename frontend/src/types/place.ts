// Mirrors backend dto/response/PlaceResponse (design.md 10.2 "Quy ước Place API").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

/** MOCK: the bundled data set · MANUAL: typed in by a user, private to its creator. */
export type PlaceProvider = 'MOCK' | 'MANUAL'

/**
 * One row of GET /places/search. It has no id yet: the place is stored only when the user picks it, and
 * provider + externalId are what names it in that request.
 */
export interface PlaceResult {
  provider: PlaceProvider
  externalId: string
  name: string
  address: string | null
  lat: number
  lng: number
  category: string | null
}

/** A point on the map, in decimal degrees. */
export interface Coordinates {
  lat: number
  lng: number
}

/**
 * Query of GET /places/search: q has 2 to 100 characters, limit 1 to 20 (default 8). lat and lng go together
 * or not at all: places around that point come first, nothing is filtered out (design.md 10.2).
 */
export interface PlaceSearchParams {
  q: string
  limit?: number
  lat?: number
  lng?: number
}

/** A place stored in the app: it has an id, the value an activity refers to. Coordinates are JSON numbers. */
export interface Place {
  id: number
  provider: PlaceProvider
  name: string
  address: string | null
  lat: number
  lng: number
  /** One of the activity types for the bundled data; a real source may send anything, or nothing */
  category: string | null
}
