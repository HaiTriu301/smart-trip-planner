// Mirrors backend dto/response/ActivityResponse (design.md 5.2, 10.2 "Quy ước Activity API").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

import type { Place } from './place'

export type ActivityType = 'SIGHTSEEING' | 'FOOD' | 'TRANSPORT' | 'ACCOMMODATION' | 'SHOPPING' | 'OTHER'

/** Times are "HH:mm" strings (LocalTime); money is a JSON number (BigDecimal). */
export interface Activity {
  id: number
  dayId: number
  title: string
  type: ActivityType
  startTime: string | null
  endTime: string | null
  orderIndex: number
  note: string | null
  costAmount: number | null
  currency: string | null
  bookingUrl: string | null
  /** Where it happens, with what the card and the map need; null when no place is attached */
  place: Place | null
  createdById: number
  version: number
  createdAt: string
  updatedAt: string
}

/** Body of POST /trips/{tripId}/days/{dayId}/activities; orderIndex is assigned by the server. */
export interface CreateActivityRequest {
  title: string
  type?: ActivityType
  startTime?: string
  endTime?: string
  note?: string
  costAmount?: number
  currency?: string
  bookingUrl?: string
  /** id of a stored place (POST /places); left out: no place on create, keep the current one on update */
  placeId?: number
}

/**
 * Body of PATCH /trips/{tripId}/activities/{activityId}: undefined keeps the value. note and bookingUrl
 * accept "" to clear; times, cost and currency cannot be cleared yet (design.md 10.2 "Quy ước Activity API").
 */
export type UpdateActivityRequest = Partial<CreateActivityRequest>
