// Mirrors backend dto/request + dto/response for /api/v1/trips (design.md 5.2, 10.2 "Quy ước Trip API").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

import type { Activity } from './activity'

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
  /** All activities of the trip, every day together */
  activityCount: number
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

/** Returned by PATCH /trips/{id}/days/{dayId}; no activities. */
export interface TripDay {
  id: number
  /** 1-based position in the trip */
  dayIndex: number
  date: string
  title: string | null
  note: string | null
}

/** A day inside GET /trips/{id}, activities sorted by orderIndex. */
export interface TripDayDetail extends TripDay {
  activities: Activity[]
}

/** GET /trips/{id}: the trip, its days and their activities in one response. */
export interface TripDetail extends TripResponse {
  days: TripDayDetail[]
}

/** Body of PATCH /trips/{id}/days/{dayId}: undefined keeps the value, "" clears it. */
export interface UpdateTripDayRequest {
  title?: string
  note?: string
}

/**
 * Body of POST /trips. The position of the destination is optional and always a pair: both numbers or neither
 * (design.md 10.2). Visibility comes with sharing (Phase 4).
 */
export interface CreateTripRequest {
  title: string
  description?: string
  coverImageUrl?: string
  destinationName?: string
  destinationLat?: number
  destinationLng?: number
  startDate: string
  endDate: string
  budgetAmount?: number
  currency?: string
}

/**
 * Body of PATCH /trips/{id}: undefined keeps the stored value. Optional fields cannot be cleared yet
 * (design.md 10.2 "Quy ước Trip API"); status has its own endpoint.
 */
export type UpdateTripRequest = Partial<CreateTripRequest>

/** GET /trips/status-counts: trips per status for the chips; every status is present, 0 when empty. */
export interface TripStatusCounts {
  total: number
  counts: Record<TripStatus, number>
}

/** Query of GET /trips; page is 0-based, undefined fields are left out of the URL. */
export interface TripListParams {
  status?: TripStatus
  q?: string
  /** "property,direction"; the backend accepts createdAt, updatedAt, startDate, title (design.md 10.2) */
  sort?: string
  page?: number
  size?: number
}
