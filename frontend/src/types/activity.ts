// Mirrors backend dto/response/ActivityResponse (design.md 5.2, 10.2 "Quy ước Activity API").
// Temporary hand-written types; replaced by generated types from OpenAPI in a later task.

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
  createdById: number
  version: number
  createdAt: string
  updatedAt: string
}
