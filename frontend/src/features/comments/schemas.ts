import { z } from 'zod'

// Mirrors backend dto/request/CreateCommentRequest (@NotBlank + @Size(max = 2000)); the messages are those of
// messages.properties, and the server's own refusal of the same rules reaches the same field.

export const COMMENT_MAX_LENGTH = 2000

export const commentSchema = z.object({
  // Measured as typed, before the server trims it, like the server does
  content: z
    .string()
    .max(COMMENT_MAX_LENGTH, `Bình luận không được vượt quá ${COMMENT_MAX_LENGTH} ký tự`)
    .refine((value) => value.trim() !== '', 'Nội dung bình luận không được để trống'),
})
export type CommentValues = z.infer<typeof commentSchema>
