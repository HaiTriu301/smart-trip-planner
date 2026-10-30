import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useIsMutating, useMutation, useQueryClient } from '@tanstack/react-query'
import { commentsKey } from '../../api/comments'
import { getTrip, updateTrip } from '../../api/trips'
import { applyFieldErrors, getApiError, getErrorMessage, isNotFound, isStaleVersion } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Modal } from '../../components/Modal'
import { classifyDateChange, type DateChange } from '../../lib/tripDates'
import { CANNOT_CLEAR_MESSAGE } from '../../lib/validation'
import { toast } from '../../stores/toastStore'
import type { TripResponse, UpdateTripRequest } from '../../types/trip'
import { findClearedFields, toTripChanges, toTripValues, tripSchema, type TripValues } from './schemas'
import { TripDateFields, TripDestinationFields, TripInfoFields } from './TripFormFields'

const FORM_FIELDS = [
  'title',
  'description',
  'coverImageUrl',
  'destinationName',
  'destinationLat',
  'destinationLng',
  'startDate',
  'endDate',
  'budgetAmount',
  'currency',
] as const satisfies readonly (keyof TripValues)[]

const DATE_HINTS: Record<Exclude<DateChange, 'none'>, string> = {
  shift: 'Dời cả chuyến đi: mọi ngày và hoạt động dời theo, không mất gì.',
  resize: 'Đổi độ dài: ngày nằm ngoài khoảng mới bị xoá cùng hoạt động của nó. Nếu ngày đó có hoạt động, bạn sẽ được hỏi lại.',
  'shift-and-resize':
    'Vừa dời vừa đổi độ dài: chỉ giữ những ngày còn nằm trong khoảng mới. Muốn dời cả chuyến rồi đổi độ dài, hãy làm hai lần: dời chuyến trước, lưu, rồi đổi độ dài sau.',
}

interface EditTripDialogProps {
  trip: TripResponse
  open: boolean
  onClose: () => void
}

/** Names the save mutation so the dialog shell can tell that the form inside it is saving. */
const updateTripKey = (tripId: number) => ['update-trip', tripId]

export function EditTripDialog({ trip, open, onClose }: EditTripDialogProps) {
  // Esc and "×" are ignored while a save is in flight: closing would unmount the form, and the answer
  // (days with activities to confirm, field errors) would have nowhere to show
  const isSaving = useIsMutating({ mutationKey: updateTripKey(trip.id) }) > 0
  return (
    <Modal open={open} title="Sửa chuyến đi" onClose={() => !isSaving && onClose()}>
      <EditTripForm trip={trip} onClose={onClose} />
    </Modal>
  )
}

/** Shown above the fields after a save refused with 409 STALE_VERSION (UI_GUIDE 10). */
const STALE_MESSAGE =
  'Chuyến đi này vừa được người khác sửa. Bấm Lưu lần nữa để ghi đè bằng nội dung của bạn, hoặc Huỷ để xem bản mới.'

/** Second question before saving: dates both shifted and resized, or the backend refused to drop activities. */
type PendingConfirm =
  | { kind: 'shift-and-resize'; body: UpdateTripRequest }
  | { kind: 'drop-activities'; body: UpdateTripRequest; message: string }

/**
 * Sends only changed fields. Date edits follow design.md rule 14.3: a mixed shift + resize asks first,
 * and 409 TRIP_DAY_HAS_ACTIVITIES asks again before resending with force=true.
 *
 * Every comparison is made with the trip as the form opened it, and that trip's version goes with the save.
 * The trip prop is live: it follows the cache, which reloads quietly (window focus, another dialog saving).
 * Compared with the reloaded copy, the fields the user left alone would differ from what somebody else just
 * saved, be sent back and overwrite it, and the new version would turn the save into an overwrite nobody
 * chose (design.md 11.5, Task 5.1).
 *
 * 409 STALE_VERSION: somebody saved the trip since; the form stays open with what the user typed, reloads the
 * trip and takes the new version, so the next save overwrites on purpose, still with only the fields the user
 * changed. 404: the trip was deleted meanwhile.
 */
function EditTripForm({ trip, onClose }: { trip: TripResponse; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [pending, setPending] = useState<PendingConfirm | null>(null)
  const [base] = useState(trip)
  // Sent with the next save; replaced only after the user was told somebody saved first
  const [version, setVersion] = useState(base.version)
  const [stale, setStale] = useState(false)
  const form = useForm<TripValues>({
    mode: 'onTouched',
    resolver: zodResolver(tripSchema),
    defaultValues: toTripValues(base),
  })
  const { handleSubmit, setError, control } = form

  const mutation = useMutation({
    mutationKey: updateTripKey(trip.id),
    mutationFn: ({ body, force }: { body: UpdateTripRequest; force: boolean }) => updateTrip(trip.id, body, force),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['trip', trip.id] }),
        queryClient.invalidateQueries({ queryKey: ['trips'] }),
        // The forecast follows the destination and the dates, both editable here
        queryClient.invalidateQueries({ queryKey: ['weather', trip.id] }),
        // Shortening the trip deletes the activities of the days cut off, and their comments with them (rule 27)
        queryClient.invalidateQueries({ queryKey: commentsKey(trip.id) }),
      ])
      onClose()
      toast.success('Đã lưu thay đổi')
    },
    // Awaited by the mutation: it stays pending while the trip reloads, so the dialog cannot be closed meanwhile
    onError: async (error, { body }) => {
      setStale(false)
      if (isNotFound(error)) {
        leaveDeletedTrip()
        return
      }
      if (isStaleVersion(error)) {
        // Also after "Vẫn đổi ngày": the question was about the old trip, so it closes with the notice
        setPending(null)
        const latest = await reloadTrip()
        if (latest === null) {
          leaveDeletedTrip()
          return
        }
        // Not read (network, access just lost): the version stays, the next save asks the server again
        if (latest) setVersion(latest.version)
        setStale(true)
        return
      }
      const apiError = getApiError(error)
      if (apiError?.errorCode === 'TRIP_DAY_HAS_ACTIVITIES') {
        // details[0].message: "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày" (design.md 10.2)
        setPending({ kind: 'drop-activities', body, message: apiError.details[0]?.message ?? apiError.message })
        return
      }
      setPending(null)
      applyFieldErrors(error, setError, FORM_FIELDS)
    },
  })

  /**
   * The trip as the server has it now, put in the cache the page reads. null: it was deleted; undefined: it could
   * not be read.
   */
  async function reloadTrip(): Promise<TripResponse | null | undefined> {
    // The other person may have moved the destination or the dates: the forecast and the comments follow them
    void queryClient.invalidateQueries({ queryKey: ['weather', trip.id] })
    void queryClient.invalidateQueries({ queryKey: commentsKey(trip.id) })
    try {
      return await queryClient.fetchQuery({
        queryKey: ['trip', trip.id],
        queryFn: () => getTrip(trip.id),
        staleTime: 0,
      })
    } catch (error) {
      return isNotFound(error) ? null : undefined
    }
  }

  function leaveDeletedTrip() {
    // The page then shows that the trip is gone (TripDetailPage)
    void queryClient.invalidateQueries({ queryKey: ['trip', trip.id] })
    void queryClient.invalidateQueries({ queryKey: ['trips'] })
    onClose()
    toast.error('Chuyến đi này vừa bị xoá')
  }

  function onSubmit(values: TripValues) {
    const cleared = findClearedFields(base, values)
    if (cleared.length > 0) {
      for (const field of cleared) {
        setError(field, { type: 'manual', message: CANNOT_CLEAR_MESSAGE })
      }
      return
    }
    const changes = toTripChanges(base, values)
    if (Object.keys(changes).length === 0) {
      onClose()
      return
    }
    const body = { ...changes, version }
    if (classifyDateChange(base.startDate, base.endDate, values.startDate, values.endDate) === 'shift-and-resize') {
      setPending({ kind: 'shift-and-resize', body })
      return
    }
    mutation.mutate({ body, force: false })
  }

  const [startDate, endDate] = useWatch({ control, name: ['startDate', 'endDate'] })
  const datesValid = Boolean(startDate && endDate && endDate >= startDate)
  const dateChange = datesValid ? classifyDateChange(base.startDate, base.endDate, startDate, endDate) : 'none'
  const isConflict = getApiError(mutation.error)?.errorCode === 'TRIP_DAY_HAS_ACTIVITIES'

  return (
    <form noValidate className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
      {stale && <Alert variant="warning">{STALE_MESSAGE}</Alert>}
      {mutation.isError && !pending && !isConflict && !stale && (
        <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>
      )}
      <TripInfoFields form={form} />
      <TripDestinationFields form={form} />
      <TripDateFields
        form={form}
        hint={
          dateChange !== 'none' && (
            <Alert variant={dateChange === 'shift' ? 'info' : 'error'}>{DATE_HINTS[dateChange]}</Alert>
          )
        }
      />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" fullWidth={false} disabled={mutation.isPending} onClick={onClose}>
          Huỷ
        </Button>
        <Button type="submit" fullWidth={false} isLoading={mutation.isPending && !pending}>
          Lưu thay đổi
        </Button>
      </div>

      <ConfirmDialog
        open={pending?.kind === 'shift-and-resize'}
        title="Vừa dời vừa đổi độ dài chuyến đi?"
        confirmLabel="Vẫn lưu"
        onCancel={() => setPending(null)}
        onConfirm={() => {
          if (!pending) return
          setPending(null)
          mutation.mutate({ body: pending.body, force: false })
        }}
      >
        <p>Những ngày còn nằm trong khoảng mới được giữ nguyên, những ngày nằm ngoài bị xoá.</p>
        <p>Nếu bạn muốn dời cả chuyến rồi đổi độ dài, hãy bấm Huỷ và làm hai lần: dời chuyến trước, đổi độ dài sau.</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={pending?.kind === 'drop-activities'}
        title="Xoá hoạt động khi đổi ngày?"
        confirmLabel="Vẫn đổi ngày"
        variant="danger"
        isLoading={mutation.isPending}
        onCancel={() => setPending(null)}
        onConfirm={() => pending && mutation.mutate({ body: pending.body, force: true })}
      >
        <p>{pending?.kind === 'drop-activities' && pending.message}.</p>
        <p>Thao tác này không hoàn tác được.</p>
      </ConfirmDialog>
    </form>
  )
}
