import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateTripDay } from '../../api/trips'
import { applyFieldErrors, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { TextAreaField } from '../../components/TextAreaField'
import { formatDate, formatWeekday } from '../../lib/format'
import type { TripDayDetail } from '../../types/trip'
import { ActivityCard } from './ActivityCard'
import { daySchema, type DayValues } from './schemas'

interface DaySectionProps {
  tripId: number
  day: TripDayDetail
}

/** One day of the timeline: header (date, title, note) and its activities in orderIndex order. */
export function DaySection({ tripId, day }: DaySectionProps) {
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
            {day.title && <p className="font-medium text-sky-800">{day.title}</p>}
            {day.note && <p className="whitespace-pre-line text-sm text-slate-600">{day.note}</p>}
          </div>
          <Button variant="secondary" className="w-auto shrink-0 text-sm" onClick={() => setIsEditing(true)}>
            Sửa ngày
          </Button>
        </header>
      )}

      {day.activities.length === 0 ? (
        <p className="text-sm text-slate-500">Chưa có hoạt động nào.</p>
      ) : (
        <div className="space-y-2">
          {day.activities.map((activity) => (
            <ActivityCard key={activity.id} activity={activity} />
          ))}
        </div>
      )}
    </section>
  )
}

interface DayEditFormProps extends DaySectionProps {
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
        <Button variant="secondary" className="w-auto" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" className="w-auto" isLoading={mutation.isPending}>
          Lưu
        </Button>
      </div>
    </form>
  )
}
