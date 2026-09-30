import { z } from 'zod'
import { countDays } from '../../lib/format'
import type { CreateTripRequest } from '../../types/trip'

// Mirrors CreateTripRequest + TripServiceImpl rules (design.md 10.2 "Quy ước Trip API", rule 14.1).
// The backend stays authoritative; these only give instant feedback with the same Vietnamese messages.

export const MAX_TRIP_DAYS = 60

const URL_PATTERN = /^https?:\/\/\S+$/
const NUMBER_PATTERN = /^-?\d+(\.\d+)?$/
// DECIMAL(15,2): up to 13 integer digits and 2 decimals
const BUDGET_PATTERN = /^\d{1,13}(\.\d{1,2})?$/

export const CURRENCIES = ['VND', 'USD', 'EUR', 'JPY', 'KRW', 'THB', 'SGD'] as const

export const tripSchema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, 'Tên chuyến đi không được để trống')
      .max(160, 'Tên chuyến đi không được vượt quá 160 ký tự'),
    description: z.string().max(5000, 'Mô tả không được vượt quá 5000 ký tự'),
    coverImageUrl: z
      .string()
      .trim()
      .max(512, 'Đường dẫn ảnh bìa không được vượt quá 512 ký tự')
      .refine((v) => v === '' || URL_PATTERN.test(v), 'Đường dẫn ảnh bìa phải bắt đầu bằng http:// hoặc https://'),
    destinationName: z.string().trim().max(200, 'Tên điểm đến không được vượt quá 200 ký tự'),
    // <input type="date"> yields "YYYY-MM-DD", or "" when empty
    startDate: z.string().min(1, 'Ngày bắt đầu không được để trống'),
    endDate: z.string().min(1, 'Ngày kết thúc không được để trống'),
    budgetAmount: z
      .string()
      .trim()
      .superRefine((v, ctx) => {
        if (v === '') return
        if (!NUMBER_PATTERN.test(v)) {
          ctx.addIssue({ code: 'custom', message: 'Ngân sách phải là một số, ví dụ 5000000' })
        } else if (v.startsWith('-')) {
          ctx.addIssue({ code: 'custom', message: 'Ngân sách không được âm' })
        } else if (!BUDGET_PATTERN.test(v)) {
          ctx.addIssue({
            code: 'custom',
            message: 'Ngân sách có tối đa 13 chữ số phần nguyên và 2 chữ số thập phân',
          })
        }
      }),
    currency: z.enum(CURRENCIES),
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
    startDate: values.startDate,
    endDate: values.endDate,
    budgetAmount: values.budgetAmount ? Number(values.budgetAmount) : undefined,
    currency: values.currency,
  }
}
