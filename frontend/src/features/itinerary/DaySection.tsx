import { useState } from 'react'
import { Pencil, Plus } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteActivity } from '../../api/activities'
import { updateTripDay } from '../../api/trips'
import { applyFieldErrors, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ExpandableText } from '../../components/ExpandableText'
import { FormField } from '../../components/FormField'
import { TextAreaField } from '../../components/TextAreaField'
import { formatDate, formatWeekday } from '../../lib/format'
import { findOverlaps } from '../../lib/timeOverlap'
import { toast } from '../../stores/toastStore'
import type { Activity } from '../../types/activity'
import type { TripDayDetail } from '../../types/trip'
import { ActivityCard } from './ActivityCard'
import { ActivityFormDialog } from './ActivityFormDialog'
import { SortableActivity, SortableDayList } from './DragDropContainer'
import { daySchema, NOTE_MAX_LENGTH, type DayValues } from './schemas'

interface DaySectionProps {
  tripId: number
  day: TripDayDetail
  /** Default currency of a new activity cost */
  tripCurrency: string
}

/**
 * The day shown on /trips/:id/days/:dayIndex (UI_GUIDE 8.1): heading with the page's main action
 * "Thêm hoạt động", then the rail of stations. One form dialog and one delete dialog for the day.
 */
export function DaySection({ tripId, day, tripCurrency }: DaySectionProps) {
  const queryClient = useQueryClient()
  const [isEditingDay, setIsEditingDay] = useState(false)
  // undefined: form closed · null: adding · Activity: editing that one
  const [editing, setEditing] = useState<Activity | null | undefined>(undefined)
  const [deleting, setDeleting] = useState<Activity | null>(null)
  const overlaps = findOverlaps(day.activities)

  const deletion = useMutation({
    mutationFn: (activity: Activity) => deleteActivity(tripId, activity.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      setDeleting(null)
      toast.success('Đã xoá hoạt động')
    },
  })

  return (
    <section aria-labelledby="day-heading" className="space-y-4">
      {isEditingDay ? (
        <DayEditForm tripId={tripId} day={day} onDone={() => setIsEditingDay(false)} />
      ) : (
        <header className="flex items-start justify-between gap-3">
          <div className="min-w-0 space-y-1">
            <h2 id="day-heading" className="scroll-mt-20 text-lg leading-[26px] font-semibold text-ink lg:scroll-mt-6">
              Ngày {day.dayIndex} · {formatWeekday(day.date)}, {formatDate(day.date)}
            </h2>
            {day.title ? (
              <p className="font-medium wrap-anywhere text-jade-dark">{day.title}</p>
            ) : (
              <p className="text-sm text-gray-400 italic">Chưa có tiêu đề</p>
            )}
            {day.note && <ExpandableText text={day.note} className="max-w-[68ch] text-sm text-gray-600" />}
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              fullWidth={false}
              aria-label={`Sửa ngày ${day.dayIndex}`}
              onClick={() => setIsEditingDay(true)}
            >
              <Pencil aria-hidden className="size-3.5" />
              Sửa
            </Button>
            {/* The page's single primary action (UI_GUIDE 7.0); a shorter label on phones */}
            <Button fullWidth={false} aria-label="Thêm hoạt động" onClick={() => setEditing(null)}>
              <Plus aria-hidden className="size-4" />
              <span className="sm:hidden">Thêm</span>
              <span className="hidden sm:inline">Thêm hoạt động</span>
            </Button>
          </div>
        </header>
      )}

      <SortableDayList dayId={day.id} activityIds={day.activities.map((a) => a.id)}>
        {day.activities.length === 0 ? (
          // An empty day invites the next step
          <li className="flex flex-col items-start gap-3 rounded-card border border-dashed border-gray-300 px-4 py-5">
            <p className="text-sm text-gray-600">Ngày này còn trống. Thêm địa điểm bạn muốn ghé.</p>
            <Button variant="secondary" fullWidth={false} onClick={() => setEditing(null)}>
              <Plus aria-hidden className="size-4" />
              Thêm hoạt động
            </Button>
          </li>
        ) : (
          day.activities.map((activity) => (
            <SortableActivity key={activity.id} activity={activity}>
              {(dragHandle) => (
                <ActivityCard
                  activity={activity}
                  dragHandle={dragHandle}
                  overlapping={overlaps.has(activity.id)}
                  onEdit={() => setEditing(activity)}
                  onDelete={() => {
                    deletion.reset()
                    setDeleting(activity)
                  }}
                />
              )}
            </SortableActivity>
          ))
        )}
      </SortableDayList>

      <ActivityFormDialog
        tripId={tripId}
        dayId={day.id}
        tripCurrency={tripCurrency}
        activity={editing ?? null}
        open={editing !== undefined}
        onClose={() => setEditing(undefined)}
      />
      <ConfirmDialog
        open={deleting !== null}
        title="Xoá hoạt động?"
        confirmLabel="Xoá hoạt động"
        variant="danger"
        isLoading={deletion.isPending}
        error={deletion.isError ? getErrorMessage(deletion.error) : undefined}
        onCancel={() => setDeleting(null)}
        onConfirm={() => deleting && deletion.mutate(deleting)}
      >
        <p>
          Hoạt động <strong className="text-ink">{deleting?.title}</strong> sẽ bị xoá khỏi ngày này. Thao tác
          này không hoàn tác được.
        </p>
      </ConfirmDialog>
    </section>
  )
}

interface DayEditFormProps {
  tripId: number
  day: TripDayDetail
  onDone: () => void
}

/** Sends both fields every time: an emptied input becomes "" and the backend clears it (design.md 10.2). */
function DayEditForm({ tripId, day, onDone }: DayEditFormProps) {
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<DayValues>({
    mode: 'onTouched',
    resolver: zodResolver(daySchema),
    defaultValues: { title: day.title ?? '', note: day.note ?? '' },
  })

  const mutation = useMutation({
    mutationFn: (values: DayValues) => updateTripDay(tripId, day.id, values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      onDone()
      toast.success('Đã lưu thay đổi')
    },
    onError: (error) => applyFieldErrors(error, setError, ['title', 'note']),
  })

  return (
    <form noValidate className="space-y-3" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <h2 className="text-lg leading-[26px] font-semibold text-ink">
        Ngày {day.dayIndex} · {formatWeekday(day.date)}, {formatDate(day.date)}
      </h2>
      {mutation.isError && <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>}
      <FormField
        label="Tiêu đề của ngày"
        placeholder="Ví dụ: Khám phá trung tâm"
        autoFocus
        error={errors.title?.message}
        {...register('title')}
      />
      <TextAreaField
        label="Ghi chú"
        hint={`Tối đa ${NOTE_MAX_LENGTH} ký tự`}
        maxLength={NOTE_MAX_LENGTH}
        error={errors.note?.message}
        {...register('note')}
      />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" fullWidth={false} onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" fullWidth={false} isLoading={mutation.isPending}>
          Lưu thay đổi
        </Button>
      </div>
    </form>
  )
}
