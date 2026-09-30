import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { login } from '../../api/auth'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { PasswordField } from '../../components/PasswordField'
import { useAuthStore } from '../../stores/authStore'
import { ResendVerificationForm } from './ResendVerificationForm'
import { loginSchema, type LoginValues } from './schemas'

/** On success only stores the session; GuestRoute then redirects to the page the user came from. */
export function LoginForm() {
  const setSession = useAuthStore((s) => s.setSession)
  const {
    register,
    handleSubmit,
    setError,
    getValues,
    formState: { errors },
  } = useForm<LoginValues>({
    mode: 'onTouched',
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: setSession,
    onError: (error) => applyFieldErrors(error, setError, ['email', 'password']),
  })

  const errorCode = getApiError(mutation.error)?.errorCode

  if (errorCode === 'EMAIL_NOT_VERIFIED') {
    return (
      <div className="space-y-4">
        <Alert variant="error">
          {getErrorMessage(mutation.error)}. Mở link trong mail xác thực, hoặc gửi lại mail bên dưới.
        </Alert>
        <ResendVerificationForm defaultEmail={getValues('email')} />
        <Button variant="secondary" onClick={() => mutation.reset()}>
          Quay lại đăng nhập
        </Button>
      </div>
    )
  }

  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      {mutation.isError && (
        <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>
      )}
      <FormField
        label="Email"
        required
        type="email"
        autoComplete="email"
        autoFocus
        error={errors.email?.message}
        {...register('email')}
      />
      <PasswordField
        label="Mật khẩu"
        required
        autoComplete="current-password"
        labelAction={
          <Link to="/forgot-password" className="text-[13px] font-medium text-jade hover:underline">
            Quên mật khẩu?
          </Link>
        }
        error={errors.password?.message}
        {...register('password')}
      />
      <Button type="submit" size="lg" isLoading={mutation.isPending}>
        Đăng nhập
      </Button>
    </form>
  )
}
