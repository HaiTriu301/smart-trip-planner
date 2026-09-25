import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { forgotPassword } from '../api/auth'
import { applyFieldErrors, getErrorMessage } from '../api/errors'
import { Alert } from '../components/Alert'
import { Button } from '../components/Button'
import { FormField } from '../components/FormField'
import { AuthLayout } from '../features/auth/AuthLayout'
import { emailOnlySchema, type EmailOnlyValues } from '../features/auth/schemas'

export function ForgotPasswordPage() {
  const {
    register,
    handleSubmit,
    setError,
    getValues,
    formState: { errors },
  } = useForm<EmailOnlyValues>({
    resolver: zodResolver(emailOnlySchema),
    defaultValues: { email: '' },
  })
  const mutation = useMutation({
    mutationFn: forgotPassword,
    onError: (error) => applyFieldErrors(error, setError, ['email']),
  })

  const footer = (
    <Link to="/login" className="font-medium text-sky-700 hover:underline">
      Quay lại đăng nhập
    </Link>
  )

  if (mutation.isSuccess) {
    // Same answer whether or not the email exists (design.md 10.2)
    return (
      <AuthLayout title="Kiểm tra hộp thư" footer={footer}>
        <Alert variant="success">
          Nếu <strong>{getValues('email')}</strong> thuộc một tài khoản đã xác thực, chúng tôi đã gửi link đặt
          lại mật khẩu. Link có hiệu lực trong 1 giờ.
        </Alert>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Quên mật khẩu"
      subtitle="Nhập email đăng ký, chúng tôi sẽ gửi link đặt lại mật khẩu"
      footer={footer}
    >
      <form
        noValidate
        className="space-y-4"
        onSubmit={handleSubmit((values) => mutation.mutate(values.email))}
      >
        {mutation.isError && <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>}
        <FormField
          label="Email"
          type="email"
          autoComplete="email"
          autoFocus
          error={errors.email?.message}
          {...register('email')}
        />
        <Button type="submit" isLoading={mutation.isPending}>
          Gửi link đặt lại
        </Button>
      </form>
    </AuthLayout>
  )
}
