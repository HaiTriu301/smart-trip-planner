import { z } from 'zod'
import { currencyCode, moneyText, urlText } from '../../lib/validation'
import type { Activity, CreateActivityRequest, UpdateActivityRequest } from '../../types/activity'
import { ACTIVITY_TYPES } from './activityType'

// Mirrors backend dto/request (UpdateTripDayRequest...), same Vietnamese messages as messages.properties.

/** Day note and activity note: a short reminder, not a document (Task 2.5) */
export const NOTE_MAX_LENGTH = 255

export const daySchema = z.object({
  title: z.string().trim().max(160, 'Tiêu đề của ngày không được vượt quá 160 ký tự'),
  note: z.string().max(NOTE_MAX_LENGTH, `Ghi chú của ngày không được vượt quá ${NOTE_MAX_LENGTH} ký tự`),
})
export type DayValues = z.infer<typeof daySchema>

// Mirrors CreateActivityRequest / UpdateActivityRequest + ActivityServiceImpl time rules (design.md 10.2
// "Quy ước Activity API"). Overlaps are not checked here: only the server knows the other activities' times.
export const activitySchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, 'Tên hoạt động không được để trống')
      .max(200, 'Tên hoạt động không được vượt quá 200 ký tự'),
    type: z.enum(ACTIVITY_TYPES),
    // <input type="time"> yields "HH:mm", or "" when empty
    startTime: z.string(),
    endTime: z.string(),
    note: z.string().max(NOTE_MAX_LENGTH, `Ghi chú của hoạt động không được vượt quá ${NOTE_MAX_LENGTH} ký tự`),
    costAmount: moneyText('Chi phí'),
    currency: currencyCode,
    bookingUrl: urlText('Đường dẫn đặt chỗ'),
    // id of the place shown in the "Địa điểm" field, null when none; the place itself is kept by the field
    placeId: z.number().nullable(),
  })
  .superRefine((v, ctx) => {
    if (v.endTime && !v.startTime) {
      ctx.addIssue({ code: 'custom', path: ['startTime'], message: 'Cần nhập giờ bắt đầu khi đã có giờ kết thúc' })
    } else if (v.startTime && v.endTime && v.endTime <= v.startTime) {
      // "HH:mm" strings compare correctly as text
      ctx.addIssue({ code: 'custom', path: ['endTime'], message: 'Giờ kết thúc phải sau giờ bắt đầu' })
    }
  })
export type ActivityValues = z.infer<typeof activitySchema>

/** Form values for a new activity (null) or an existing one; a new cost defaults to the trip currency. */
export function toActivityValues(activity: Activity | null, tripCurrency: string): ActivityValues {
  return {
    title: activity?.title ?? '',
    type: activity?.type ?? 'OTHER',
    startTime: activity?.startTime ?? '',
    endTime: activity?.endTime ?? '',
    note: activity?.note ?? '',
    costAmount: activity?.costAmount == null ? '' : String(activity.costAmount),
    currency: activity?.currency ?? tripCurrency,
    bookingUrl: activity?.bookingUrl ?? '',
    placeId: activity?.place?.id ?? null,
  }
}

/** Empty inputs are left out; the currency only travels with a cost. */
export function toCreateActivityRequest(values: ActivityValues): CreateActivityRequest {
  const hasCost = values.costAmount !== ''
  return {
    title: values.title,
    type: values.type,
    startTime: values.startTime || undefined,
    endTime: values.endTime || undefined,
    note: values.note.trim() || undefined,
    costAmount: hasCost ? Number(values.costAmount) : undefined,
    currency: hasCost ? values.currency : undefined,
    bookingUrl: values.bookingUrl || undefined,
    placeId: values.placeId ?? undefined,
  }
}

const UNCLEARABLE_FIELDS = ['startTime', 'endTime', 'costAmount'] as const

/** Times and cost that held a value and were emptied: PATCH cannot clear them yet (design.md 10.2). */
export function findClearedActivityFields(
  activity: Activity,
  values: ActivityValues,
): (typeof UNCLEARABLE_FIELDS)[number][] {
  const before = toActivityValues(activity, '')
  return UNCLEARABLE_FIELDS.filter((field) => before[field] !== '' && values[field] === '')
}

/**
 * Only the fields that differ from the stored activity; note and bookingUrl emptied are sent as "" to clear them.
 * placeId travels only when the place changed: sending back the place an activity already has is not needed,
 * and for a collaborator it could be refused (a place added by hand belongs to its creator, rule 14.19).
 * A place that was removed and not replaced is sent as clearPlace; the two never travel together.
 */
export function toUpdateActivityRequest(activity: Activity, values: ActivityValues): UpdateActivityRequest {
  const body: UpdateActivityRequest = {}
  if (values.title !== activity.title) body.title = values.title
  if (values.type !== activity.type) body.type = values.type
  if (values.startTime && values.startTime !== activity.startTime) body.startTime = values.startTime
  if (values.endTime && values.endTime !== activity.endTime) body.endTime = values.endTime
  const note = values.note.trim()
  if (note !== (activity.note ?? '')) body.note = note
  if (values.bookingUrl !== (activity.bookingUrl ?? '')) body.bookingUrl = values.bookingUrl
  if (values.costAmount !== '') {
    const cost = Number(values.costAmount)
    if (cost !== activity.costAmount) body.costAmount = cost
    if (values.currency !== activity.currency) body.currency = values.currency
  }
  if (values.placeId === null) {
    if (activity.place) body.clearPlace = true
  } else if (values.placeId !== activity.place?.id) {
    body.placeId = values.placeId
  }
  return body
}
