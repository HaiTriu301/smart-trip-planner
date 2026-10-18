import { z } from 'zod'
import { emailSchema } from '../auth/schemas'
import { EXPIRY_CHOICES } from '../../lib/linkExpiry'

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
 * "Hết hạn" (Task 4.9): a quick pick, or "Chọn ngày…" with a calendar day from today on, today being the
 * account's (useToday). The picked day is only checked, and only needed, while "Chọn ngày…" is chosen. The server
 * checks again against its own clock; its refusal comes back on expiresAt.
 */
export function shareLinkSchema(today: string) {
  return z
    .object({
      expiry: z.enum(EXPIRY_CHOICES),
      expiresAt: z.string(),
    })
    .superRefine(({ expiry, expiresAt }, ctx) => {
      if (expiry !== 'custom') return
      const message =
        expiresAt === ''
          ? 'Chọn ngày hết hạn trên lịch'
          : !ISO_DATE.test(expiresAt)
            ? 'Ngày không hợp lệ'
            : expiresAt < today
              ? 'Ngày hết hạn phải từ hôm nay trở đi'
              : null
      if (message) ctx.addIssue({ code: 'custom', path: ['expiresAt'], message })
    })
}
export type ShareLinkValues = z.infer<ReturnType<typeof shareLinkSchema>>
