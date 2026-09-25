import { z } from 'zod'

// Mirrors the Bean Validation rules on backend dto/request (RegisterRequest, ResetPasswordRequest...).
// The backend stays authoritative; these only give instant feedback with the same Vietnamese messages.

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[\x21-\x7E]+$/

export const emailSchema = z
  .string()
  .min(1, 'Email không được để trống')
  .max(255, 'Email không được vượt quá 255 ký tự')
  .pipe(z.email('Email không đúng định dạng'))

const newPasswordSchema = z
  .string()
  .min(1, 'Mật khẩu không được để trống')
  .min(8, 'Mật khẩu phải dài từ 8 đến 72 ký tự')
  .max(72, 'Mật khẩu phải dài từ 8 đến 72 ký tự')
  .regex(
    PASSWORD_PATTERN,
    'Mật khẩu phải có ít nhất một chữ hoa, một chữ thường, một chữ số và không chứa khoảng trắng',
  )

const confirmPasswordSchema = z.string().min(1, 'Vui lòng nhập lại mật khẩu')

const PASSWORD_MISMATCH = 'Mật khẩu xác nhận không khớp'

export const loginSchema = z.object({
  email: emailSchema,
  // No strength rules on login: any string is compared against the stored hash
  password: z.string().min(1, 'Mật khẩu không được để trống'),
})
export type LoginValues = z.infer<typeof loginSchema>

export const registerSchema = z
  .object({
    fullName: z
      .string()
      .trim()
      .min(1, 'Họ tên không được để trống')
      .max(120, 'Họ tên không được vượt quá 120 ký tự'),
    email: emailSchema,
    password: newPasswordSchema,
    confirmPassword: confirmPasswordSchema,
  })
  .refine((v) => v.password === v.confirmPassword, {
    path: ['confirmPassword'],
    message: PASSWORD_MISMATCH,
  })
export type RegisterValues = z.infer<typeof registerSchema>

export const emailOnlySchema = z.object({ email: emailSchema })
export type EmailOnlyValues = z.infer<typeof emailOnlySchema>

export const resetPasswordSchema = z
  .object({
    newPassword: newPasswordSchema,
    confirmPassword: confirmPasswordSchema,
  })
  .refine((v) => v.newPassword === v.confirmPassword, {
    path: ['confirmPassword'],
    message: PASSWORD_MISMATCH,
  })
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>
