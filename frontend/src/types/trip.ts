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

/** Returned by POST /trips and PATCH /trips/{id}; money is a JSON number (BigDecimal). */
export interface TripResponse {
  id: number
  ownerId: number
  title: string
  slug: string
  description: string | null
  coverImageUrl: string | null
  destinationName: string | null
  destinationLat: number | null
  destinationLng: number | null
  startDate: string
  endDate: string
  budgetAmount: number | null
  currency: string
  status: TripStatus
  visibility: TripVisibility
  version: number
  createdAt: string
  updatedAt: string
}

/** Body of POST /trips. Coordinates come with the map picker (Task 3.4), visibility with sharing (Phase 4). */
export interface CreateTripRequest {
  title: string
  description?: string
  coverImageUrl?: string
  destinationName?: string
  startDate: string
  endDate: string
  budgetAmount?: number
  currency?: string
}

/** Query of GET /trips; page is 0-based, undefined fields are left out of the URL. */
export interface TripListParams {
  status?: TripStatus
  q?: string
  page?: number
  size?: number
}
