import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { resendVerification } from '../../api/auth'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { emailOnlySchema, type EmailOnlyValues } from './schemas'

interface ResendVerificationFormProps {
  defaultEmail?: string
}

/** The backend always answers 200 (design.md 10.2), so success never confirms the email exists. */
export function ResendVerificationForm({ defaultEmail = '' }: ResendVerificationFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<EmailOnlyValues>({
    resolver: zodResolver(emailOnlySchema),
    defaultValues: { email: defaultEmail },
  })
  const mutation = useMutation({ mutationFn: resendVerification })

  return (
    <form
      noValidate
      className="space-y-3"
      onSubmit={handleSubmit((values) => mutation.mutate(values.email))}
    >
      {mutation.isSuccess && (
        <Alert variant="success">
          Nếu email này chưa được xác thực, một mail xác thực mới đã được gửi. Link cũ không còn dùng được.
        </Alert>
      )}
      {mutation.isError && <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>}
      <FormField
        label="Email"
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <Button type="submit" variant="secondary" isLoading={mutation.isPending}>
        Gửi lại mail xác thực
      </Button>
    </form>
  )
}
