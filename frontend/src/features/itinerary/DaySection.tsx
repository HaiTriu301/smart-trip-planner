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
 * One day of the timeline (UI_GUIDE 8.1, cách a): heading, then the rail of stations. Days are separated by
 * a thin line, not boxed in cards (UI_GUIDE 1: not every block is a card).
 */
export function DaySection({ tripId, day, tripCurrency }: DaySectionProps) {
  const [isEditing, setIsEditing] = useState(false)

  return (
    <section id={`day-${day.id}`} aria-labelledby={`day-${day.id}-heading`} className="scroll-mt-16 space-y-4 py-6 first:pt-0 lg:scroll-mt-6">
      {isEditing ? (
        <DayEditForm tripId={tripId} day={day} onDone={() => setIsEditing(false)} />
      ) : (
        <header className="flex items-start justify-between gap-3">
          <div className="min-w-0 space-y-1">
            <h2 id={`day-${day.id}-heading`} className="text-lg leading-[26px] font-semibold text-ink">
              Ngày {day.dayIndex} · {formatWeekday(day.date)}, {formatDate(day.date)}
            </h2>
            {day.title ? (
              <p className="font-medium wrap-anywhere text-jade-dark">{day.title}</p>
            ) : (
              <p className="text-sm text-gray-400 italic">Chưa có tiêu đề</p>
            )}
            {day.note && <ExpandableText text={day.note} className="max-w-[68ch] text-sm text-gray-600" />}
          </div>
          <Button
            variant="ghost"
            size="sm"
            fullWidth={false}
            className="shrink-0"
            aria-label={`Sửa ngày ${day.dayIndex}`}
            onClick={() => setIsEditing(true)}
          >
            <Pencil aria-hidden className="size-3.5" />
            Sửa
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
    <>
      <SortableDayList dayId={day.id} activityIds={day.activities.map((a) => a.id)}>
        {day.activities.length === 0 ? (
          // An empty day invites the next step and still accepts activities dragged from another day
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
      {day.activities.length > 0 && (
        // Lined up with the cards: past the time column and the rail
        <div className="pl-16">
          <Button variant="dashed" onClick={() => setEditing(null)}>
            <Plus aria-hidden className="size-4" />
            Thêm hoạt động
          </Button>
        </div>
      )}

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
