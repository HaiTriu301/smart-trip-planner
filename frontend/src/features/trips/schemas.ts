import { z } from 'zod'
import { countDays } from '../../lib/format'
import { currencyCode, moneyText, urlText } from '../../lib/validation'
import type { CreateTripRequest, TripResponse, UpdateTripRequest } from '../../types/trip'

// Mirrors CreateTripRequest + TripServiceImpl rules (design.md 10.2 "Quy ước Trip API", rule 14.1).
// The backend stays authoritative; these only give instant feedback with the same Vietnamese messages.

export const MAX_TRIP_DAYS = 60
export const DESCRIPTION_MAX_LENGTH = 1000

export const tripSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, 'Tên chuyến đi không được để trống')
      .max(160, 'Tên chuyến đi không được vượt quá 160 ký tự'),
    description: z
      .string()
      .max(DESCRIPTION_MAX_LENGTH, `Mô tả không được vượt quá ${DESCRIPTION_MAX_LENGTH} ký tự`),
    coverImageUrl: urlText('Đường dẫn ảnh bìa'),
    destinationName: z.string().trim().max(200, 'Tên điểm đến không được vượt quá 200 ký tự'),
    // Position of the destination, set by picking a place: the form always writes the two together, so they
    // are both numbers or both null (the backend refuses one without the other)
    destinationLat: z.number().nullable(),
    destinationLng: z.number().nullable(),
    // <input type="date"> yields "YYYY-MM-DD", or "" when empty
    startDate: z.string().min(1, 'Ngày bắt đầu không được để trống'),
    endDate: z.string().min(1, 'Ngày kết thúc không được để trống'),
    budgetAmount: moneyText('Ngân sách'),
    currency: currencyCode,
  })
  .superRefine((v, ctx) => {
    // ISO dates compare correctly as strings
    if (v.endDate < v.startDate) {
      ctx.addIssue({
        code: 'custom',
        path: ['endDate'],
        message: 'Ngày kết thúc phải bằng hoặc sau ngày bắt đầu',
      })
    } else if (countDays(v.startDate, v.endDate) > MAX_TRIP_DAYS) {
      ctx.addIssue({
        code: 'custom',
        path: ['endDate'],
        message: `Chuyến đi dài tối đa ${MAX_TRIP_DAYS} ngày`,
      })
    }
  })
export type TripValues = z.infer<typeof tripSchema>

/** Empty optional inputs are left out so the backend applies its defaults. */
export function toCreateTripRequest(values: TripValues): CreateTripRequest {
  return {
    title: values.title,
    description: values.description.trim() || undefined,
    coverImageUrl: values.coverImageUrl || undefined,
    destinationName: values.destinationName || undefined,
    destinationLat: values.destinationLat ?? undefined,
    destinationLng: values.destinationLng ?? undefined,
    startDate: values.startDate,
    endDate: values.endDate,
    budgetAmount: values.budgetAmount ? Number(values.budgetAmount) : undefined,
    currency: values.currency,
  }
}

/** Form values of an existing trip, for the edit form. */
export function toTripValues(trip: TripResponse): TripValues {
  return {
    title: trip.title,
    description: trip.description ?? '',
    coverImageUrl: trip.coverImageUrl ?? '',
    destinationName: trip.destinationName ?? '',
    destinationLat: trip.destinationLat,
    destinationLng: trip.destinationLng,
    startDate: trip.startDate,
    endDate: trip.endDate,
    budgetAmount: trip.budgetAmount === null ? '' : String(trip.budgetAmount),
    currency: trip.currency,
  }
}

const CLEARABLE_FIELDS = ['description', 'coverImageUrl', 'destinationName', 'budgetAmount'] as const

/**
 * Optional fields that held a value and were emptied. PATCH cannot clear them yet (design.md 10.2):
 * sending nothing would silently keep the old value, so the form reports them instead.
 */
export function findClearedFields(trip: TripResponse, values: TripValues): (typeof CLEARABLE_FIELDS)[number][] {
  const before = toTripValues(trip)
  return CLEARABLE_FIELDS.filter((field) => before[field].trim() !== '' && values[field].trim() === '')
}

/** Only the fields that differ from the stored trip; an empty object means nothing to save. */
export function toUpdateTripRequest(trip: TripResponse, values: TripValues): UpdateTripRequest {
  const next = toCreateTripRequest(values)
  const body: UpdateTripRequest = {}
  if (next.title !== trip.title) body.title = next.title
  if (next.description !== undefined && next.description !== trip.description) body.description = next.description
  if (next.coverImageUrl !== undefined && next.coverImageUrl !== trip.coverImageUrl) body.coverImageUrl = next.coverImageUrl
  if (next.destinationName !== undefined && next.destinationName !== trip.destinationName) {
    body.destinationName = next.destinationName
  }
  // A moved position is sent as a pair, even when only one of the two numbers changed
  if (
    next.destinationLat !== undefined &&
    next.destinationLng !== undefined &&
    (next.destinationLat !== trip.destinationLat || next.destinationLng !== trip.destinationLng)
  ) {
    body.destinationLat = next.destinationLat
    body.destinationLng = next.destinationLng
  }
  if (next.startDate !== trip.startDate) body.startDate = next.startDate
  if (next.endDate !== trip.endDate) body.endDate = next.endDate
  if (next.budgetAmount !== undefined && next.budgetAmount !== trip.budgetAmount) body.budgetAmount = next.budgetAmount
  if (next.currency !== trip.currency) body.currency = next.currency
  return body
}
