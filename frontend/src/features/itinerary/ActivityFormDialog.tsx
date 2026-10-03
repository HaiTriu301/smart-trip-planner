import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query'
import { createActivity, updateActivity } from '../../api/activities'
import { applyFieldErrors, getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Modal } from '../../components/Modal'
import { CANNOT_CLEAR_MESSAGE } from '../../lib/validation'
import { toast } from '../../stores/toastStore'
import type { Activity, CreateActivityRequest, UpdateActivityRequest } from '../../types/activity'
import { ActivityFormFields } from './ActivityFormFields'
import {
  activitySchema,
  findClearedActivityFields,
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
  'placeId',
] as const satisfies readonly (keyof ActivityValues)[]

interface ActivityFormDialogProps {
  tripId: number
  dayId: number
  tripCurrency: string
  /** null: add a new activity to the day */
  activity: Activity | null
  open: boolean
  onClose: () => void
}

/** Names the save mutation so the dialog shell can tell that the form inside it is saving. */
const saveActivityKey = (tripId: number) => ['save-activity', tripId]

export function ActivityFormDialog({ open, activity, onClose, ...props }: ActivityFormDialogProps) {
  // Esc and "×" are ignored while a save is in flight: closing would unmount the form, and the answer
  // (time conflict to confirm, field errors) would have nowhere to show
  const isSaving = useIsMutating({ mutationKey: saveActivityKey(props.tripId) }) > 0
  return (
    <Modal
      open={open}
      size="lg"
      title={activity ? 'Sửa hoạt động' : 'Thêm hoạt động'}
      onClose={() => !isSaving && onClose()}
    >
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
  const form = useForm<ActivityValues>({
    mode: 'onTouched',
    resolver: zodResolver(activitySchema),
    defaultValues: toActivityValues(activity, tripCurrency),
  })
  const { handleSubmit, setError } = form

  const mutation = useMutation({
    mutationKey: saveActivityKey(tripId),
    mutationFn: ({ request, allowOverlap }: { request: SaveRequest; allowOverlap: boolean }) =>
      request.kind === 'create'
        ? createActivity(tripId, dayId, request.body, allowOverlap)
        : updateActivity(tripId, request.activityId, request.body, allowOverlap),
    onSuccess: async (_saved, { request }) => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      onClose()
      toast.success(request.kind === 'create' ? 'Đã thêm hoạt động' : 'Đã lưu thay đổi')
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

  const isConflict = getApiError(mutation.error)?.errorCode === 'ACTIVITY_TIME_CONFLICT'

  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
      {mutation.isError && !overlap && !isConflict && (
        <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>
      )}
      <ActivityFormFields form={form} initialPlace={activity?.place ?? null} />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" fullWidth={false} disabled={mutation.isPending} onClick={onClose}>
          Huỷ
        </Button>
        <Button type="submit" fullWidth={false} isLoading={mutation.isPending && !overlap}>
          {activity ? 'Lưu thay đổi' : 'Thêm hoạt động'}
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
