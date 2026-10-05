import { useState } from 'react'
import { Flag, Pencil, Plus } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deleteActivity } from '../../api/activities'
import { getDayRoute } from '../../api/routes'
import { updateTripDay } from '../../api/trips'
import { applyFieldErrors, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { BackToTopButton } from '../../components/BackToTopButton'
import { Badge } from '../../components/Badge'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ExpandableText } from '../../components/ExpandableText'
import { FormField } from '../../components/FormField'
import { TextAreaField } from '../../components/TextAreaField'
import { useToday } from '../../hooks/useToday'
import { formatDate, formatWeekday } from '../../lib/format'
import { findOverlaps } from '../../lib/timeOverlap'
import { dayStatus } from '../../lib/today'
import { legsAfter } from '../../lib/travelLegs'
import { toast } from '../../stores/toastStore'
import type { Activity } from '../../types/activity'
import type { TripDayDetail } from '../../types/trip'
import { ActivityCard } from './ActivityCard'
import { ActivityFormDialog } from './ActivityFormDialog'
import { MoveToDayDialog } from './MoveToDayDialog'
import { TravelLeg } from './TravelLeg'
import { SortableActivity, SortableDayList } from './DragDropContainer'
import { daySchema, NOTE_MAX_LENGTH, type DayValues } from './schemas'
import { DayWeatherLine } from '../weather/DayWeatherLine'

interface DaySectionProps {
  tripId: number
  day: TripDayDetail
  /** Every day of the trip, for "Chuyển sang ngày…" */
  days: TripDayDetail[]
  /** Default currency of a new activity cost */
  tripCurrency: string
  onMoveToDay: (activityId: number, dayId: number) => void
}

/**
 * The day shown on /trips/:id/days/:dayIndex (UI_GUIDE 8.1): heading with the page's main action
 * "Thêm hoạt động", then the rail of stations. One form dialog and one delete dialog for the day.
 * <p>
 * The heading is a card in two parts (Task 3.9, mockup "Teal Voyage"). Top: "Ngày N" with its badge, the full
 * date under it, and the two buttons. The name of the day is short on purpose: with the map column in place the
 * middle column can be as narrow as 368px, and a long line next to buttons that do not shrink breaks over
 * several lines (BUG-UI-010). Bottom, under a rule: the day's own title, the number of places and the note.
 * <p>
 * On wide screens the card is sticky (UI_GUIDE 8.1 "Ngày dài"): the 24px of page background above it line it up
 * with the pinned day list on the left and cover the cards scrolling underneath. The negative top margin
 * cancels that padding while nothing is scrolled, so the page looks the same at rest.
 */
export function DaySection({ tripId, day, days, tripCurrency, onMoveToDay }: DaySectionProps) {
  const queryClient = useQueryClient()
  const [isEditingDay, setIsEditingDay] = useState(false)
  // undefined: form closed · null: adding · Activity: editing that one
  const [editing, setEditing] = useState<Activity | null | undefined>(undefined)
  const [deleting, setDeleting] = useState<Activity | null>(null)
  const [moving, setMoving] = useState<Activity | null>(null)
  const overlaps = findOverlaps(day.activities)
  const status = dayStatus(day.date, useToday())

  // Travel between the activities that have a place; one without a place is passed over, and the leg then
  // names where it arrives. Its own key, not under ['trip', id]; a day with fewer than two places has no leg,
  // so nothing is asked
  const placeCount = day.activities.filter((activity) => activity.place).length
  const { data: route } = useQuery({
    queryKey: ['route', tripId, day.id],
    queryFn: () => getDayRoute(tripId, day.id),
    enabled: placeCount >= 2,
  })
  const legs = legsAfter(day.activities, route?.legs ?? [])
  const titleOf = (activityId: number) => day.activities.find((a) => a.id === activityId)?.title

  const deletion = useMutation({
    mutationFn: (activity: Activity) => deleteActivity(tripId, activity.id),
    onSuccess: async () => {
      // The two neighbours of the deleted activity now follow each other: a leg the server has not sent yet
      void queryClient.invalidateQueries({ queryKey: ['route', tripId] })
      await queryClient.invalidateQueries({ queryKey: ['trip', tripId] })
      setDeleting(null)
      toast.success('Đã xoá hoạt động')
    },
  })

  return (
    // id day-start: target of "Đầu ngày" and of the scroll after a day change. Not the heading: on wide screens it
    // sits in the pinned header, always "in view", so scrolling to it would do nothing. The scroll margins land
    // the section under the sticky day chips (phones) or 24px down, where the pinned header rests (wide screens).
    <section
      id="day-start"
      aria-labelledby="day-heading"
      tabIndex={-1}
      className="scroll-mt-20 space-y-4 focus:outline-none lg:scroll-mt-6"
    >
      {/* The header of the day, a card (UI_GUIDE 8.1). On wide screens it stays pinned: the wrapper carries the
          page background above and below the card, so the activities scrolling underneath never show around it */}
      <div className="lg:sticky lg:top-0 lg:z-10 lg:-mt-6 lg:bg-paper lg:pt-6 lg:pb-3">
        {isEditingDay ? (
          <div className="rounded-card bg-white p-4 shadow-md xl:p-5">
            <DayEditForm tripId={tripId} day={day} onDone={() => setIsEditingDay(false)} />
          </div>
        ) : (
          <header className="rounded-card bg-white shadow-md">
            <div className="flex items-start justify-between gap-3 p-4 xl:px-5">
              <div className="min-w-0">
                {/* The badge never breaks in two: short of room, it goes to the next line as a whole */}
                <h2
                  id="day-heading"
                  className="flex flex-wrap items-center gap-x-2 gap-y-1 text-2xl leading-8 font-bold tracking-[-0.01em] text-ink"
                >
                  <span>Ngày {day.dayIndex}</span>
                  {status === 'today' && <Badge tone="brand">Hôm nay</Badge>}
                  {status === 'past' && <Badge tone="muted">Đã qua</Badge>}
                </h2>
                <p className="tabular mt-0.5 text-sm text-gray-500">
                  {formatWeekday(day.date)}, {formatDate(day.date)}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  fullWidth={false}
                  aria-label={`Sửa ngày ${day.dayIndex}`}
                  onClick={() => setIsEditingDay(true)}
                >
                  <Pencil aria-hidden className="size-3.5" />
                  Sửa
                </Button>
                {/* The page's single primary action (UI_GUIDE 7.0). The full label only where the column has
                    room for it: between phones and three columns, and from 1280px */}
                <Button fullWidth={false} aria-label="Thêm hoạt động" onClick={() => setEditing(null)}>
                  <Plus aria-hidden className="size-4" />
                  <span className="sm:hidden lg:inline xl:hidden">Thêm</span>
                  <span className="hidden sm:inline lg:hidden xl:inline">Thêm hoạt động</span>
                </Button>
              </div>
            </div>
            {/* What the day is about: its own title, how many places it has, its note */}
            <div className="space-y-1.5 border-t border-tide px-4 py-3 xl:px-5">
              <div className="flex items-start justify-between gap-3">
                {day.title ? (
                  <p className="flex min-w-0 items-start gap-2 font-medium text-jade-dark">
                    <Flag aria-hidden className="mt-1 size-4 shrink-0" />
                    <span className="min-w-0 wrap-anywhere">{day.title}</span>
                  </p>
                ) : (
                  <p className="flex items-center gap-2 text-sm text-gray-400 italic">
                    <Flag aria-hidden className="size-4 shrink-0" />
                    Chưa có tiêu đề
                  </p>
                )}
                <div className="flex shrink-0 items-center gap-1">
                  {placeCount > 0 && <span className="tabular text-xs leading-8 text-gray-500">{placeCount} địa điểm</span>}
                  {/* Wide screens only, where this header stays pinned; phones have the floating button */}
                  <div className="hidden lg:block">
                    <BackToTopButton placement="inline" label="Đầu ngày" targetId="day-start" />
                  </div>
                </div>
              </div>
              <DayWeatherLine tripId={tripId} dayId={day.id} />
              {day.note && <ExpandableText text={day.note} className="max-w-[68ch] text-sm text-gray-600" />}
            </div>
          </header>
        )}
      </div>

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
          day.activities.map((activity) => {
            const shown = legs.get(activity.id)
            const travel = shown && (
              <TravelLeg leg={shown.leg} destination={shown.direct ? undefined : titleOf(shown.leg.toActivityId)} />
            )
            return (
              <SortableActivity key={activity.id} activity={activity} below={travel}>
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
                    onMoveToDay={days.length > 1 ? () => setMoving(activity) : undefined}
                  />
                )}
              </SortableActivity>
            )
          })
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
      <MoveToDayDialog activity={moving} days={days} onMove={onMoveToDay} onClose={() => setMoving(null)} />
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
