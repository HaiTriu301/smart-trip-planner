import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link2 } from 'lucide-react'
import { createShareLink, createShareLinkKey, shareLinksKey } from '../../api/shareLinks'
import { applyFieldErrors, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { useToday } from '../../hooks/useToday'
import { expiryInstant } from '../../lib/linkExpiry'
import { DEFAULT_TIME_ZONE } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import { toast } from '../../stores/toastStore'
import { shareLinkSchema, type ShareLinkValues } from './schemas'

/**
 * "Tạo liên kết" with an optional "Hết hạn sau ngày" (UI_GUIDE 15.3). A secondary button: the dialog's primary
 * action is "Mời". The chosen day becomes its last millisecond in the account's time zone (lib/linkExpiry); left
 * empty, the link never expires. The new link appears at the top of the list.
 */
export function CreateShareLinkForm({ tripId }: { tripId: number }) {
  const queryClient = useQueryClient()
  const today = useToday()
  const timeZone = useAuthStore((state) => state.user?.timezone) ?? DEFAULT_TIME_ZONE
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<ShareLinkValues>({
    mode: 'onTouched',
    resolver: zodResolver(shareLinkSchema(today)),
    defaultValues: { expiresAt: '' },
  })

  const mutation = useMutation({
    mutationKey: createShareLinkKey(tripId),
    mutationFn: ({ expiresAt }: ShareLinkValues) =>
      createShareLink(tripId, expiresAt === '' ? undefined : expiryInstant(expiresAt, timeZone)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: shareLinksKey(tripId) })
      reset()
      toast.success('Đã tạo liên kết')
    },
    onError: (error) => applyFieldErrors(error, setError, ['expiresAt']),
  })

  // A refusal on the date shows under the field; anything else (server down, trip deleted) above the form
  const bannerError = mutation.isError && !errors.expiresAt ? getErrorMessage(mutation.error) : undefined

  return (
    <form
      noValidate
      className="space-y-3"
      onSubmit={handleSubmit((values) => {
        mutation.reset()
        mutation.mutate(values)
      })}
    >
      {bannerError && <Alert variant="error">{bannerError}</Alert>}
      <div className="grid gap-3 sm:grid-cols-[220px_auto] sm:items-start">
        <FormField
          label="Hết hạn sau ngày"
          type="date"
          min={today}
          hint="Bỏ trống: liên kết không hết hạn"
          error={errors.expiresAt?.message}
          {...register('expiresAt')}
        />
        {/* Down by the height of a field label (18px + 6px), so the button lines up with the date field */}
        <div className="sm:pt-6">
          <Button type="submit" variant="secondary" fullWidth={false} isLoading={mutation.isPending}>
            <Link2 aria-hidden className="size-4" />
            Tạo liên kết
          </Button>
        </div>
      </div>
    </form>
  )
}
