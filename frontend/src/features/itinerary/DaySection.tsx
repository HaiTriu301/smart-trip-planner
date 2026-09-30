import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteActivity } from '../../api/activities'
import { updateTripDay } from '../../api/trips'
import { applyFieldErrors, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FormField } from '../../components/FormField'
import { TextAreaField } from '../../components/TextAreaField'
import { formatDate, formatWeekday } from '../../lib/format'
import type { Activity } from '../../types/activity'
import type { TripDayDetail } from '../../types/trip'
import { ActivityCard } from './ActivityCard'
import { ActivityFormDialog } from './ActivityFormDialog'
import { SortableActivity, SortableDayList } from './DragDropContainer'
import { daySchema, type DayValues } from './schemas'

interface DaySectionProps {
  tripId: number
  day: TripDayDetail
  /** Default currency of a new activity cost */
  tripCurrency: string
}

/** One day of the timeline: header (date, title, note) and its activities in orderIndex order. */
export function DaySection({ tripId, day, tripCurrency }: DaySectionProps) {
  const [isEditing, setIsEditing] = useState(false)

  return (
    <section id={`day-${day.id}`} className="scroll-mt-4 space-y-3 rounded-xl border border-slate-200 bg-slate-100/60 p-4">
      {isEditing ? (
        <DayEditForm tripId={tripId} day={day} onDone={() => setIsEditing(false)} />
      ) : (
        <header className="flex items-start justify-between gap-3">
          <div className="min-w-0 space-y-1">
            <h3 className="font-semibold text-slate-800">
              Ngày {day.dayIndex} · {formatWeekday(day.date)}, {formatDate(day.date)}
            </h3>
            {day.title ? (
              <p className="font-medium text-sky-800">{day.title}</p>
            ) : (
              <p className="italic text-slate-400">Chưa có tiêu đề</p>
            )}
            {day.note && <p className="whitespace-pre-line text-sm text-slate-600">{day.note}</p>}
          </div>
          <Button
            variant="ghost"
            size="sm"
            fullWidth={false}
            className="shrink-0"
            aria-label={`Sửa ngày ${day.dayIndex}`}
            onClick={() => setIsEditing(true)}
          >
            <span aria-hidden>✎</span>&nbsp;Sửa
          </Button>
        </header>
      )}

      <DayActivities tripId={tripId} day={day} tripCurrency={tripCurrency} />
    </section>
  )
}

/** Activities of the day with add / edit / delete. One form dialog and one delete dialog per day. */
function DayActivities({ tripId, day, tripCurrency }: DaySectionProps) {
  const queryClient = useQueryClient()
  // undefined: form closed · null: adding · Activity: editing that one
  const [editing, setEditing] = useState<Activity | null | undefined>(undefined)
  const [deleting, setDeleting] = useState<Activity | null>(null)

  const deletion = useMutation({
    mutationFn: (activity: Activity) => deleteActivity(tripId, activity.id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      setDeleting(null)
    },
  })

  return (
    <>
      <SortableDayList dayId={day.id} activityIds={day.activities.map((a) => a.id)}>
        {day.activities.length === 0 ? (
          <p className="px-1 py-3 text-sm text-slate-500">Chưa có hoạt động nào. Có thể kéo hoạt động từ ngày khác vào đây.</p>
        ) : (
          day.activities.map((activity) => (
            <SortableActivity key={activity.id} activity={activity}>
              {(dragHandle) => (
                <ActivityCard
                  activity={activity}
                  dragHandle={dragHandle}
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
      <Button variant="ghost" size="sm" fullWidth={false} onClick={() => setEditing(null)}>
        + Thêm hoạt động
      </Button>

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
          Hoạt động <strong className="text-slate-800">{deleting?.title}</strong> sẽ bị xoá khỏi ngày này. Thao tác
          này không hoàn tác được.
        </p>
      </ConfirmDialog>
    </>
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
    resolver: zodResolver(daySchema),
    defaultValues: { title: day.title ?? '', note: day.note ?? '' },
  })

  const mutation = useMutation({
    mutationFn: (values: DayValues) => updateTripDay(tripId, day.id, values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      onDone()
    },
    onError: (error) => applyFieldErrors(error, setError, ['title', 'note']),
  })

  return (
    <form noValidate className="space-y-3" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <h3 className="font-semibold text-slate-800">
        Ngày {day.dayIndex} · {formatWeekday(day.date)}, {formatDate(day.date)}
      </h3>
      {mutation.isError && <Alert variant="error">{getErrorMessage(mutation.error)}</Alert>}
      <FormField
        label="Tiêu đề của ngày"
        placeholder="Ví dụ: Khám phá trung tâm"
        autoFocus
        error={errors.title?.message}
        {...register('title')}
      />
      <TextAreaField label="Ghi chú" error={errors.note?.message} {...register('note')} />
      <div className="flex justify-end gap-2">
        <Button variant="secondary" fullWidth={false} onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" fullWidth={false} isLoading={mutation.isPending}>
          Lưu
        </Button>
      </div>
    </form>
  )
}
