import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link2 } from 'lucide-react'
import { createShareLink, createShareLinkKey, shareLinksKey } from '../../api/shareLinks'
import { applyFieldErrors, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { ChoiceChips } from '../../components/ChoiceChips'
import { DatePickerField } from '../../components/DatePickerField'
import { useToday } from '../../hooks/useToday'
import { formatDate } from '../../lib/format'
import { expiryDateFor, expiryInstant } from '../../lib/linkExpiry'
import { DEFAULT_TIME_ZONE } from '../../lib/today'
import { useAuthStore } from '../../stores/authStore'
import { toast } from '../../stores/toastStore'
import { shareLinkSchema, type ShareLinkValues } from './schemas'

const EXPIRY_OPTIONS = [
  { value: 'never', label: 'Không hết hạn' },
  { value: '7', label: '7 ngày' },
  { value: '30', label: '30 ngày' },
  { value: 'custom', label: 'Chọn ngày…' },
] as const

/**
 * "Tạo liên kết" with its "Hết hạn" (UI_GUIDE 15.2; quick picks from Task 4.9): never (the default), 7 or 30 days
 * from today, or a day picked on a calendar. A line under the picks says until when the link will work. A secondary
 * button: the dialog's primary action is "Mời". The last day becomes its last millisecond in the account's time
 * zone (lib/linkExpiry). The new link appears at the top of the list.
 */
export function CreateShareLinkForm({ tripId }: { tripId: number }) {
  const queryClient = useQueryClient()
  const today = useToday()
  const timeZone = useAuthStore((state) => state.user?.timezone) ?? DEFAULT_TIME_ZONE
  // "Chọn ngày…" pressed with the mouse or a finger: the calendar opens with the date field
  const [openCalendar, setOpenCalendar] = useState(false)
  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<ShareLinkValues>({
    mode: 'onTouched',
    resolver: zodResolver(shareLinkSchema(today)),
    defaultValues: { expiry: 'never', expiresAt: '' },
  })
  const [expiry, expiresAt] = useWatch({ control, name: ['expiry', 'expiresAt'] })
  const lastDay = expiryDateFor(expiry, today, expiresAt)

  const mutation = useMutation({
    mutationKey: createShareLinkKey(tripId),
    mutationFn: (values: ShareLinkValues) => {
      const day = expiryDateFor(values.expiry, today, values.expiresAt)
      return createShareLink(tripId, day === '' ? undefined : expiryInstant(day, timeZone))
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: shareLinksKey(tripId) })
      reset()
      setOpenCalendar(false)
      toast.success('Đã tạo liên kết')
    },
    onError: (error) => applyFieldErrors(error, setError, ['expiresAt']),
  })

  // A refusal on the date shows under the field; anything else (server down, trip deleted) above the form
  const bannerError = mutation.isError && !errors.expiresAt ? getErrorMessage(mutation.error) : undefined
  const choosingDay = expiry === 'custom'

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
      <ChoiceChips
        label="Hết hạn"
        options={EXPIRY_OPTIONS}
        registration={register('expiry')}
        onChipClick={(value) => setOpenCalendar(value === 'custom')}
        // With a quick pick there is no date field: a refusal of the server on the date shows here
        error={choosingDay ? undefined : errors.expiresAt?.message}
        hint={
          choosingDay
            ? undefined
            : lastDay
              ? `Dùng được đến hết ngày ${formatDate(lastDay)}`
              : 'Ai có liên kết đều xem được cho tới khi bạn thu hồi'
        }
      />
      {choosingDay && (
        <div className="sm:w-[220px]">
          <Controller
            name="expiresAt"
            control={control}
            render={({ field }) => (
              <DatePickerField
                label="Dùng được đến hết ngày"
                required
                today={today}
                minDate={today}
                defaultOpen={openCalendar}
                error={errors.expiresAt?.message}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                ref={field.ref}
              />
            )}
          />
        </div>
      )}
      <Button type="submit" variant="secondary" fullWidth={false} isLoading={mutation.isPending}>
        <Link2 aria-hidden className="size-4" />
        Tạo liên kết
      </Button>
    </form>
  )
}
