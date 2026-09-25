import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { register as registerAccount } from '../../api/auth'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { registerSchema, type RegisterValues } from './schemas'

interface RegisterFormProps {
  onRegistered: (email: string) => void
}

export function RegisterForm({ onRegistered }: RegisterFormProps) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { fullName: '', email: '', password: '', confirmPassword: '' },
  })

  const mutation = useMutation({
    mutationFn: registerAccount,
    onSuccess: (user) => onRegistered(user.email),
    onError: (error) => {
      if (getApiError(error)?.errorCode === 'EMAIL_ALREADY_EXISTS') {
        setError('email', { type: 'server', message: getErrorMessage(error) })
        return
      }
      applyFieldErrors(error, setError, ['fullName', 'email', 'password', 'confirmPassword'])
    },
  })

  const showBanner =
    mutation.isError && getApiError(mutation.error)?.errorCode !== 'EMAIL_ALREADY_EXISTS'

  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      {showBanner && <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>}
      <FormField
        label="Họ tên"
        autoComplete="name"
        autoFocus
        error={errors.fullName?.message}
        {...register('fullName')}
      />
      <FormField
        label="Email"
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <FormField
        label="Mật khẩu"
        type="password"
        autoComplete="new-password"
        error={errors.password?.message}
        {...register('password')}
      />
      <FormField
        label="Nhập lại mật khẩu"
        type="password"
        autoComplete="new-password"
        error={errors.confirmPassword?.message}
        {...register('confirmPassword')}
      />
      <p className="text-xs text-slate-500">
        Mật khẩu 8–72 ký tự, có chữ hoa, chữ thường, chữ số và không chứa khoảng trắng.
      </p>
      <Button type="submit" isLoading={mutation.isPending}>
        Tạo tài khoản
      </Button>
    </form>
  )
}
