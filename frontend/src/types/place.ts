// Mirrors backend dto/response/PlaceResponse (design.md 10.2 "Quy ước Place API").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

/** MOCK: the bundled data set · MANUAL: typed in by a user, private to its creator. */
export type PlaceProvider = 'MOCK' | 'MANUAL'

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
