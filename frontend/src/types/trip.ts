// Mirrors backend dto/request + dto/response for /api/v1/trips (design.md 5.2, 10.2 "Quy ước Trip API").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

export type TripStatus = 'DRAFT' | 'PLANNED' | 'ONGOING' | 'COMPLETED' | 'ARCHIVED'
export type TripVisibility = 'PRIVATE' | 'LINK' | 'PUBLIC'

/** One row of GET /trips. Dates are ISO "YYYY-MM-DD" strings (LocalDate). */
export interface TripSummary {
  id: number
  title: string
  slug: string
  coverImageUrl: string | null
  destinationName: string | null
  startDate: string
  endDate: string
  status: TripStatus
  visibility: TripVisibility
  createdAt: string
}

/** Query of GET /trips; page is 0-based, undefined fields are left out of the URL. */
export interface TripListParams {
  status?: TripStatus
  q?: string
  page?: number
  size?: number
}
