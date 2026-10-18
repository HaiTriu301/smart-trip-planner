import { useId } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Eye, Pencil } from 'lucide-react'
import { inviteMember, inviteMemberKey } from '../../api/members'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { SegmentedControl } from '../../components/SegmentedControl'
import { toast } from '../../stores/toastStore'
import { inviteSchema, MEMBER_ROLE_OPTIONS, type InviteValues } from './schemas'

const FORM_FIELDS = ['email', 'role'] as const

// Pencil like every "Sửa" of the app, an eye for looking only
const ROLE_SEGMENTS = MEMBER_ROLE_OPTIONS.map((option) => ({
  ...option,
  icon: option.value === 'EDITOR' ? Pencil : Eye,
}))

/** A refusal the form shows under a field: "already a member", or a field rule of the server (owner's own email). */
function landsOnField(error: unknown): boolean {
  const apiError = getApiError(error)
  if (apiError?.errorCode === 'MEMBER_ALREADY_EXISTS') return true
  return (apiError?.details ?? []).some((detail) => FORM_FIELDS.some((field) => field === detail.field))
}

/**
 * "Mời thành viên" (UI_GUIDE 15.3): email, role and the dialog's primary button "Mời", on one row from 640px.
 * The server sends the mail; the new person shows up in the list once the trip is reloaded. The email field is
 * emptied for the next invitation, the role is kept.
 */
export function InviteMemberForm({ tripId }: { tripId: number }) {
  const queryClient = useQueryClient()
  const headingId = useId()
  const {
    register,
    handleSubmit,
    setError,
    resetField,
    formState: { errors },
  } = useForm<InviteValues>({
    mode: 'onTouched',
    resolver: zodResolver(inviteSchema),
    defaultValues: { email: '', role: 'EDITOR' },
  })

  const mutation = useMutation({
    mutationKey: inviteMemberKey(tripId),
    mutationFn: (values: InviteValues) => inviteMember(tripId, values),
    onSuccess: async (member) => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      resetField('email')
      toast.success(`Đã gửi lời mời tới ${member.email}`)
    },
    onError: (error) => {
      // Already a member: the server's sentence belongs under the email, like the owner's own email (400)
      if (getApiError(error)?.errorCode === 'MEMBER_ALREADY_EXISTS') {
        setError('email', { type: 'server', message: getErrorMessage(error) })
        return
      }
      applyFieldErrors(error, setError, FORM_FIELDS)
    },
  })

  // Anything else (server down, trip deleted meanwhile) goes to a banner above the fields
  const bannerError = mutation.isError && !landsOnField(mutation.error) ? getErrorMessage(mutation.error) : undefined

  return (
    <section aria-labelledby={headingId} className="space-y-2">
      <h3 id={headingId} className="text-[15px] leading-6 font-semibold text-ink">
        Mời thành viên
      </h3>
      <form
        noValidate
        className="space-y-3"
        onSubmit={handleSubmit((values) => {
          mutation.reset()
          mutation.mutate(values)
        })}
      >
        {bannerError && <Alert variant="error">{bannerError}</Alert>}
        <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:items-start">
          <FormField
            label="Email"
            type="email"
            autoComplete="off"
            placeholder="ten@vidu.com"
            required
            error={errors.email?.message}
            {...register('email')}
          />
          <SegmentedControl
            label="Vai trò"
            options={ROLE_SEGMENTS}
            error={errors.role?.message}
            registration={register('role')}
          />
          {/* Down by the height of a field label (18px + 6px), so the button lines up with the inputs */}
          <div className="sm:pt-6">
            <Button type="submit" fullWidth={false} isLoading={mutation.isPending}>
              Mời
            </Button>
          </div>
        </div>
      </form>
    </section>
  )
}
