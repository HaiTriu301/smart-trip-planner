import { z } from 'zod'
import { emailSchema } from '../auth/schemas'

// Mirrors backend dto/request/InviteMemberRequest; the backend stays authoritative (owner's own email,
// already a member) and its messages reach the same fields.

export const MEMBER_ROLE_OPTIONS = [
  { value: 'EDITOR', label: 'Chỉnh sửa' },
  { value: 'VIEWER', label: 'Chỉ xem' },
] as const

export const inviteSchema = z.object({
  // A pasted address often carries a space at either end: trimmed before the format check
  email: z.string().trim().pipe(emailSchema),
  role: z.enum(['EDITOR', 'VIEWER']),
})
export type InviteValues = z.infer<typeof inviteSchema>

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

/**
 * "Hết hạn sau ngày": empty (never expires) or a calendar day from today on, today being the account's
 * (useToday). The server checks again against its own clock.
 */
export function shareLinkSchema(today: string) {
  return z.object({
    expiresAt: z
      .string()
      .refine((value) => value === '' || ISO_DATE.test(value), 'Ngày không hợp lệ')
      .refine((value) => value === '' || value >= today, 'Ngày hết hạn phải từ hôm nay trở đi'),
  })
}
export type ShareLinkValues = z.infer<ReturnType<typeof shareLinkSchema>>
