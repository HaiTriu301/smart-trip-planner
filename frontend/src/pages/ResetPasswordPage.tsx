import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { resetPassword } from '../api/auth'
import { applyFieldErrors, getApiError, getErrorMessage } from '../api/errors'
import { Alert } from '../components/Alert'
import { Button } from '../components/Button'
import { FormField } from '../components/FormField'
import { AuthLayout } from '../features/auth/AuthLayout'
import { resetPasswordSchema, type ResetPasswordValues } from '../features/auth/schemas'

const forgotLink = (
  <Link to="/forgot-password" className="font-medium text-sky-700 hover:underline">
    Yêu cầu link mới
  </Link>
)

/** Landing page of the mailed link: /reset-password?token=... */
export function ResetPasswordPage() {
  const token = useSearchParams()[0].get('token')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  })
  const mutation = useMutation({
    mutationFn: resetPassword,
    onError: (error) => applyFieldErrors(error, setError, ['newPassword', 'confirmPassword']),
  })

  if (!token) {
    return (
      <AuthLayout title="Đặt lại mật khẩu" footer={forgotLink}>
        <Alert variant="error">Link đặt lại mật khẩu không hợp lệ. Hãy mở đúng link trong mail.</Alert>
      </AuthLayout>
    )
  }

  if (mutation.isSuccess) {
    return (
      <AuthLayout title="Đặt lại mật khẩu">
        <div className="space-y-4">
          <Alert variant="success">
            Đã đổi mật khẩu. Mọi thiết bị đang đăng nhập đã bị đăng xuất; hãy đăng nhập lại bằng mật khẩu mới.
          </Alert>
          <p className="text-center">
            <Link to="/login" className="font-medium text-sky-700 hover:underline">
              Đến trang đăng nhập
            </Link>
          </p>
        </div>
      </AuthLayout>
    )
  }

  const invalidToken = getApiError(mutation.error)?.errorCode === 'INVALID_TOKEN'

  return (
    <AuthLayout title="Đặt lại mật khẩu" subtitle="Nhập mật khẩu mới cho tài khoản của bạn" footer={forgotLink}>
      <form
        noValidate
        className="space-y-4"
        onSubmit={handleSubmit((values) => mutation.mutate({ token, ...values }))}
      >
        {mutation.isError && (
          <Alert variant="error">
            {getErrorMessage(mutation.error)}
            {invalidToken && '. Link có thể đã hết hạn hoặc đã được dùng, hãy yêu cầu link mới.'}
          </Alert>
        )}
        <FormField
          label="Mật khẩu mới"
          type="password"
          autoComplete="new-password"
          autoFocus
          error={errors.newPassword?.message}
          {...register('newPassword')}
        />
        <FormField
          label="Nhập lại mật khẩu mới"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <Button type="submit" isLoading={mutation.isPending}>
          Đổi mật khẩu
        </Button>
      </form>
    </AuthLayout>
  )
}
