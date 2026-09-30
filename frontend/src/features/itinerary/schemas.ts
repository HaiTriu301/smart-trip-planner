import { z } from 'zod'

// Mirrors backend dto/request (UpdateTripDayRequest...), same Vietnamese messages as messages.properties.

export const daySchema = z.object({
  title: z.string().trim().max(160, 'Tiêu đề của ngày không được vượt quá 160 ký tự'),
  note: z.string().max(5000, 'Ghi chú của ngày không được vượt quá 5000 ký tự'),
})
export type DayValues = z.infer<typeof daySchema>
