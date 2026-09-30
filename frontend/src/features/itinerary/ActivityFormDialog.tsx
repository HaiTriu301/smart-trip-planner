import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createActivity, updateActivity } from '../../api/activities'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FormField } from '../../components/FormField'
import { Modal } from '../../components/Modal'
import { SelectField } from '../../components/SelectField'
import { TextAreaField } from '../../components/TextAreaField'
import { CANNOT_CLEAR_MESSAGE, currencyOptions } from '../../lib/validation'
import type { Activity, CreateActivityRequest, UpdateActivityRequest } from '../../types/activity'
import { ACTIVITY_TYPES, ACTIVITY_TYPE_LABELS } from './activityType'
import {
  activitySchema,
  findClearedActivityFields,
  NOTE_MAX_LENGTH,
  toActivityValues,
  toCreateActivityRequest,
  toUpdateActivityRequest,
  type ActivityValues,
} from './schemas'

const FORM_FIELDS = [
  'title',
  'type',
  'startTime',
  'endTime',
  'note',
  'costAmount',
  'currency',
  'bookingUrl',
] as const satisfies readonly (keyof ActivityValues)[]

const TYPE_OPTIONS = ACTIVITY_TYPES.map((type) => ({ value: type, label: ACTIVITY_TYPE_LABELS[type] }))

interface ActivityFormDialogProps {
  tripId: number
  dayId: number
  tripCurrency: string
  /** null: add a new activity to the day */
  activity: Activity | null
  open: boolean
  onClose: () => void
}

export function ActivityFormDialog({ open, activity, onClose, ...props }: ActivityFormDialogProps) {
  return (
    <Modal open={open} size="lg" title={activity ? 'Sửa hoạt động' : 'Thêm hoạt động'} onClose={onClose}>
      <ActivityForm activity={activity} onClose={onClose} {...props} />
    </Modal>
  )
}

type SaveRequest =
  | { kind: 'create'; body: CreateActivityRequest }
  | { kind: 'update'; activityId: number; body: UpdateActivityRequest }

/** A save refused with 409 ACTIVITY_TIME_CONFLICT, waiting for the user to confirm the overlap. */
interface PendingOverlap {
  request: SaveRequest
  messages: string[]
}

/**
 * Create or partial update. 409 ACTIVITY_TIME_CONFLICT lists the overlapping activities and asks
 * "Vẫn lưu?"; yes resends the same body with allowOverlap=true (design.md rule 14.4).
 */
function ActivityForm({ tripId, dayId, tripCurrency, activity, onClose }: Omit<ActivityFormDialogProps, 'open'>) {
  const queryClient = useQueryClient()
  const [overlap, setOverlap] = useState<PendingOverlap | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<ActivityValues>({
    mode: 'onTouched',
    resolver: zodResolver(activitySchema),
    defaultValues: toActivityValues(activity, tripCurrency),
  })

  const mutation = useMutation({
    mutationFn: ({ request, allowOverlap }: { request: SaveRequest; allowOverlap: boolean }) =>
      request.kind === 'create'
        ? createActivity(tripId, dayId, request.body, allowOverlap)
        : updateActivity(tripId, request.activityId, request.body, allowOverlap),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      onClose()
    },
    onError: (error, { request }) => {
      const apiError = getApiError(error)
      if (apiError?.errorCode === 'ACTIVITY_TIME_CONFLICT') {
        // details: 'Trùng giờ với hoạt động "Ăn sáng" (09:00 - 10:00)'
        const messages = apiError.details.flatMap((d) => (d.message ? [d.message] : []))
        setOverlap({ request, messages: messages.length > 0 ? messages : [apiError.message] })
        return
      }
      setOverlap(null)
      applyFieldErrors(error, setError, FORM_FIELDS)
    },
  })

  function onSubmit(values: ActivityValues) {
    if (!activity) {
      mutation.mutate({ request: { kind: 'create', body: toCreateActivityRequest(values) }, allowOverlap: false })
      return
    }
    const cleared = findClearedActivityFields(activity, values)
    if (cleared.length > 0) {
      for (const field of cleared) setError(field, { type: 'manual', message: CANNOT_CLEAR_MESSAGE })
      return
    }
    const body = toUpdateActivityRequest(activity, values)
    if (Object.keys(body).length === 0) {
      onClose()
      return
    }
    mutation.mutate({ request: { kind: 'update', activityId: activity.id, body }, allowOverlap: false })
  }

  const currency = useWatch({ control, name: 'currency' })
  const isConflict = getApiError(mutation.error)?.errorCode === 'ACTIVITY_TIME_CONFLICT'

  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
      {mutation.isError && !overlap && !isConflict && (
        <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>
      )}
      <FormField label="Tên hoạt động" required autoFocus error={errors.title?.message} {...register('title')} />
      <SelectField label="Loại" options={TYPE_OPTIONS} error={errors.type?.message} {...register('type')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Giờ bắt đầu" type="time" error={errors.startTime?.message} {...register('startTime')} />
        <FormField label="Giờ kết thúc" type="time" error={errors.endTime?.message} {...register('endTime')} />
      </div>
      <div className="grid gap-4 sm:grid-cols-[1fr_8rem]">
        <FormField
          label="Chi phí"
          inputMode="decimal"
          placeholder="Ví dụ: 350000"
          error={errors.costAmount?.message}
          {...register('costAmount')}
        />
        <SelectField
          label="Tiền tệ"
          options={currencyOptions(currency)}
          error={errors.currency?.message}
          {...register('currency')}
        />
      </div>
      <FormField
        label="Link đặt chỗ"
        type="url"
        placeholder="https://..."
        error={errors.bookingUrl?.message}
        {...register('bookingUrl')}
      />
      <TextAreaField
        label="Ghi chú"
        hint={`Tối đa ${NOTE_MAX_LENGTH} ký tự`}
        maxLength={NOTE_MAX_LENGTH}
        error={errors.note?.message}
        {...register('note')}
      />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" fullWidth={false} disabled={mutation.isPending} onClick={onClose}>
          Huỷ
        </Button>
        <Button type="submit" fullWidth={false} isLoading={mutation.isPending && !overlap}>
          {activity ? 'Lưu' : 'Thêm'}
        </Button>
      </div>

      <ConfirmDialog
        open={overlap !== null}
        title="Trùng giờ với hoạt động khác"
        confirmLabel="Vẫn lưu"
        isLoading={mutation.isPending}
        onCancel={() => setOverlap(null)}
        onConfirm={() => overlap && mutation.mutate({ request: overlap.request, allowOverlap: true })}
      >
        <ul className="list-disc space-y-1 pl-5">
          {overlap?.messages.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
        <p>Bạn vẫn muốn lưu hoạt động này?</p>
      </ConfirmDialog>
    </form>
  )
}
