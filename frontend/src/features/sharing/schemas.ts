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
