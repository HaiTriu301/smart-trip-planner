import { createContext, useContext, useState, type ReactNode } from 'react'
import {
  closestCorners,
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
  type UniqueIdentifier,
} from '@dnd-kit/core'
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { reorderActivities } from '../../api/activities'
import { getApiError, getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { reorderItemsForDrop, type ReorderItem } from '../../lib/orderIndex'
import type { Activity } from '../../types/activity'
import type { TripDayDetail, TripDetail } from '../../types/trip'
import { ActivityCard } from './ActivityCard'

// dnd-kit ids: activities and days share one id space, so they are prefixed ("a-12", "d-3")
const activityKey = (id: number) => `a-${id}`
const dayKey = (id: number) => `d-${id}`

function parseKey(key: UniqueIdentifier): { kind: 'activity' | 'day'; id: number } {
  const [prefix, id] = String(key).split('-')
  return { kind: prefix === 'd' ? 'day' : 'activity', id: Number(id) }
}

/** The day a key points at: the day itself, or the day holding that activity. */
function findDay(days: TripDayDetail[], key: UniqueIdentifier): TripDayDetail | undefined {
  const { kind, id } = parseKey(key)
  return kind === 'day' ? days.find((d) => d.id === id) : days.find((d) => d.activities.some((a) => a.id === id))
}

/** New days with the activity taken out of its day and inserted into `targetDayId` at `index`. */
function moveToDay(days: TripDayDetail[], activityId: number, targetDayId: number, index: number): TripDayDetail[] {
  const moving = days.flatMap((d) => d.activities).find((a) => a.id === activityId)
  if (!moving) return days
  return days.map((day) => {
    const rest = day.activities.filter((a) => a.id !== activityId)
    if (day.id !== targetDayId) return rest.length === day.activities.length ? day : { ...day, activities: rest }
    const at = Math.min(index, rest.length)
    return { ...day, activities: [...rest.slice(0, at), { ...moving, dayId: targetDayId }, ...rest.slice(at)] }
  })
}

/** Writes the new dayId / orderIndex of the sent items into the days, for the optimistic view. */
function applyItems(days: TripDayDetail[], items: ReorderItem[]): TripDayDetail[] {
  const byId = new Map(items.map((item) => [item.activityId, item]))
  return days.map((day) => ({
    ...day,
    activities: day.activities.map((a) => {
      const item = byId.get(a.id)
      return item ? { ...a, dayId: item.dayId, orderIndex: item.orderIndex } : a
    }),
  }))
}

/** True while a reorder is saving or waiting for the overlap question: dragging is paused. */
const DragDisabledContext = createContext(false)

interface PendingReorder {
  items: ReorderItem[]
  /** Days as they were before the drop, restored on failure or cancel */
  snapshot: TripDayDetail[]
}

interface DragDropContainerProps {
  tripId: number
  days: TripDayDetail[]
  /** Renders the days; during a drag they already show the activity at its hover position */
  children: (days: TripDayDetail[]) => ReactNode
}

/**
 * Drag and drop of activities within a day and across days (design.md 15, 10.2 "Quy ước Reorder").
 * On drop the view updates at once (optimistic), then PUT /activities/reorder sends only the moved activity;
 * the returned days replace the affected ones. 409 ACTIVITY_TIME_CONFLICT asks before resending with
 * allowOverlap=true; cancelling or any other error puts every activity back where it was.
 */
export function DragDropContainer({ tripId, days, children }: DragDropContainerProps) {
  const queryClient = useQueryClient()
  // Working copy while dragging, so hovering over another day previews the move without touching the cache
  const [dragDays, setDragDays] = useState<TripDayDetail[] | null>(null)
  const [activeId, setActiveId] = useState<number | null>(null)
  const [conflict, setConflict] = useState<(PendingReorder & { messages: string[] }) | null>(null)

  const sensors = useSensors(
    // A few pixels of movement before a drag starts, so a plain click on the handle does nothing
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  function setCachedDays(update: (current: TripDayDetail[]) => TripDayDetail[]) {
    queryClient.setQueryData<TripDetail>(['trip', tripId], (trip) => trip && { ...trip, days: update(trip.days) })
  }

  const mutation = useMutation({
    mutationFn: ({ items, allowOverlap }: PendingReorder & { allowOverlap: boolean }) =>
      reorderActivities(tripId, items, allowOverlap),
    onSuccess: (affected) => {
      setConflict(null)
      // The response carries the final orderIndex, including a backend renumbering
      setCachedDays((current) => current.map((day) => affected.find((d) => d.id === day.id) ?? day))
    },
    onError: (error, { items, snapshot }) => {
      const apiError = getApiError(error)
      if (apiError?.errorCode === 'ACTIVITY_TIME_CONFLICT') {
        // details: 'Trùng giờ với hoạt động "..." (09:00 - 10:00)', one per moved activity that overlaps
        const messages = apiError.details.flatMap((d) => (d.message ? [d.message] : []))
        setConflict({ items, snapshot, messages: messages.length > 0 ? messages : [apiError.message] })
        return
      }
      setConflict(null)
      setCachedDays(() => snapshot)
    },
  })

  function handleDragStart({ active }: DragStartEvent) {
    mutation.reset()
    setActiveId(parseKey(active.id).id)
    setDragDays(days)
  }

  /** Hovering over another day moves the activity there in the working copy. */
  function handleDragOver({ active, over }: DragOverEvent) {
    if (!over || !dragDays) return
    const from = findDay(dragDays, active.id)
    const to = findDay(dragDays, over.id)
    if (!from || !to || from.id === to.id) return
    const overTarget = parseKey(over.id)
    const index =
      overTarget.kind === 'day' ? to.activities.length : to.activities.findIndex((a) => a.id === overTarget.id)
    setDragDays(moveToDay(dragDays, parseKey(active.id).id, to.id, index))
  }

  function handleDragEnd({ active, over }: DragEndEvent) {
    const working = dragDays
    setDragDays(null)
    setActiveId(null)
    if (!over || !working) return

    const movedId = parseKey(active.id).id
    let finalDays = working
    const day = findDay(working, active.id)
    const overTarget = parseKey(over.id)
    // Same day: dnd-kit only animated the order, apply it now
    if (day && overTarget.kind === 'activity' && overTarget.id !== movedId && findDay(working, over.id)?.id === day.id) {
      const from = day.activities.findIndex((a) => a.id === movedId)
      const to = day.activities.findIndex((a) => a.id === overTarget.id)
      finalDays = working.map((d) => (d.id === day.id ? { ...d, activities: arrayMove(d.activities, from, to) } : d))
    }

    const target = findDay(finalDays, active.id)
    const origin = findDay(days, active.id)
    if (!target || !origin) return
    const unchanged =
      target.id === origin.id &&
      target.activities.findIndex((a) => a.id === movedId) === origin.activities.findIndex((a) => a.id === movedId)
    if (unchanged) return

    const items = reorderItemsForDrop(target.id, target.activities, movedId)
    // Stop a background refetch from overwriting the optimistic view with the old order
    void queryClient.cancelQueries({ queryKey: ['trip', tripId] })
    setCachedDays(() => applyItems(finalDays, items))
    mutation.mutate({ items, snapshot: days, allowOverlap: false })
  }

  function handleDragCancel() {
    setDragDays(null)
    setActiveId(null)
  }

  const shownDays = dragDays ?? days
  const activeActivity = activeId === null ? undefined : shownDays.flatMap((d) => d.activities).find((a) => a.id === activeId)

  return (
    <DragDisabledContext.Provider value={mutation.isPending || conflict !== null}>
      {mutation.isError && !conflict && (
        <Alert variant="error">Không sắp xếp được: {getErrorMessage(mutation.error)}. Các hoạt động đã về chỗ cũ.</Alert>
      )}
      <DndContext
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
        onDragCancel={handleDragCancel}
      >
        {children(shownDays)}
        <DragOverlay>
          {activeActivity && <ActivityCard activity={activeActivity} dragHandle={<DragHandleIcon />} />}
        </DragOverlay>
      </DndContext>

      <ConfirmDialog
        open={conflict !== null}
        title="Trùng giờ ở ngày mới"
        confirmLabel="Vẫn chuyển"
        isLoading={mutation.isPending}
        onCancel={() => {
          if (conflict) setCachedDays(() => conflict.snapshot)
          setConflict(null)
          mutation.reset()
        }}
        onConfirm={() => conflict && mutation.mutate({ items: conflict.items, snapshot: conflict.snapshot, allowOverlap: true })}
      >
        <ul className="list-disc space-y-1 pl-5">
          {conflict?.messages.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
        <p>Bạn vẫn muốn chuyển hoạt động sang ngày này? Chọn Huỷ để đưa hoạt động về chỗ cũ.</p>
      </ConfirmDialog>
    </DragDisabledContext.Provider>
  )
}

interface SortableDayListProps {
  dayId: number
  activityIds: number[]
  children: ReactNode
}

/** The activity list of one day: a sortable list and a drop target, so an empty day can receive activities. */
export function SortableDayList({ dayId, activityIds, children }: SortableDayListProps) {
  const { setNodeRef, isOver } = useDroppable({ id: dayKey(dayId) })
  return (
    <SortableContext id={dayKey(dayId)} items={activityIds.map(activityKey)} strategy={verticalListSortingStrategy}>
      <div
        ref={setNodeRef}
        className={`min-h-12 space-y-2 rounded-card transition-colors ${isOver ? 'bg-jade-light ring-2 ring-jade/25' : ''}`}
      >
        {children}
      </div>
    </SortableContext>
  )
}

interface SortableActivityProps {
  activity: Activity
  /** Receives the drag handle to place inside the card; only the handle starts a drag */
  children: (dragHandle: ReactNode) => ReactNode
}

export function SortableActivity({ activity, children }: SortableActivityProps) {
  const disabled = useContext(DragDisabledContext)
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: activityKey(activity.id),
    disabled,
  })

  const handle = (
    <button
      type="button"
      ref={setActivatorNodeRef}
      {...attributes}
      {...listeners}
      aria-label={`Kéo để sắp xếp ${activity.title}`}
      className="cursor-grab touch-none rounded px-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700 active:cursor-grabbing aria-disabled:cursor-not-allowed aria-disabled:opacity-40"
    >
      <DragHandleIcon />
    </button>
  )

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={isDragging ? 'opacity-40' : undefined}
    >
      {children(handle)}
    </div>
  )
}

function DragHandleIcon() {
  return (
    <span aria-hidden className="text-lg leading-none text-gray-400">
      ⠿
    </span>
  )
}
