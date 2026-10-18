// Mirrors backend dto/response/PublicTripResponse for GET /api/v1/public/trips/{token} (design.md 10.2).
// Only what a guest needs to follow the itinerary: no ids, no owner, no members, no costs, no booking links,
// no notes of the days.

import type { ActivityType } from './activity'

export interface PublicPlace {
  name: string
  address: string | null
  lat: number
  lng: number
}

/** Times are "HH:mm" strings; null = not scheduled. */
export interface PublicActivity {
  title: string
  type: ActivityType
  startTime: string | null
  endTime: string | null
  note: string | null
  place: PublicPlace | null
}

/** Activities in the order the owner arranged them; an empty list for a day without any. */
export interface PublicDay {
  dayIndex: number
  /** "YYYY-MM-DD" */
  date: string
  title: string | null
  activities: PublicActivity[]
}

export interface PublicTrip {
  title: string
  description: string | null
  destinationName: string | null
  destinationLat: number | null
  destinationLng: number | null
  startDate: string
  endDate: string
  /** Every day of the trip in calendar order */
  days: PublicDay[]
}
