import { createContext, useContext, useState, type ReactNode } from 'react'
import {
  closestCorners,
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  pointerWithin,
  useDroppable,
  useSensor,
  useSensors,
  type CollisionDetection,
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
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { reorderItemsForDrop, type ReorderItem } from '../../lib/orderIndex'
import { toast } from '../../stores/toastStore'
import type { Activity } from '../../types/activity'
import type { TripDayDetail, TripDetail } from '../../types/trip'
import { ChevronDown, ChevronUp, GripVertical } from 'lucide-react'
import { ActivityCard } from './ActivityCard'
import { ACTIVITY_ROUTE } from './activityType'

// dnd-kit ids share one id space, so they are prefixed: "a-12" an activity, "d-3" the rail of the day on screen,
// "n-3" the entry of a day in the day list (drop there to move the activity to that day)
const activityKey = (id: number) => `a-${id}`
const dayKey = (id: number) => `d-${id}`
const navKey = (id: number) => `n-${id}`

type KeyKind = 'activity' | 'day' | 'nav'
const KINDS: Record<string, KeyKind> = { a: 'activity', d: 'day', n: 'nav' }

function parseKey(key: UniqueIdentifier): { kind: KeyKind; id: number } {
  const [prefix, id] = String(key).split('-')
  return { kind: KINDS[prefix] ?? 'activity', id: Number(id) }
}

/** The day a key points at: the day itself (rail or day-list entry), or the day holding that activity. */
function findDay(days: TripDayDetail[], key: UniqueIdentifier): TripDayDetail | undefined {
  const { kind, id } = parseKey(key)
  return kind === 'activity' ? days.find((d) => d.activities.some((a) => a.id === id)) : days.find((d) => d.id === id)
}

/**
 * The day list only counts when the pointer is right on an entry, so dragging inside the day never snaps to it;
 * everything else (the rail and its cards) uses the usual closest-corners rule.
 */
const collisionDetection: CollisionDetection = (args) => {
  const isNav = (id: UniqueIdentifier) => String(id).startsWith('n-')
  const onDayList = pointerWithin({ ...args, droppableContainers: args.droppableContainers.filter((c) => isNav(c.id)) })
  if (onDayList.length > 0) return onDayList
  return closestCorners({ ...args, droppableContainers: args.droppableContainers.filter((c) => !isNav(c.id)) })
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

interface ReorderContextValue {
  /** True while a reorder is saving or waiting for the overlap question: dragging and moving are paused */
  disabled: boolean
  /** Move one step up or down inside its day (one day per page: leaving the day would hide the activity) */
  move: (activityId: number, direction: -1 | 1) => void
  /** False for the first activity of its day going up and the last one going down */
  canMove: (activityId: number, direction: -1 | 1) => boolean
}

const ReorderContext = createContext<ReorderContextValue>({
  disabled: false,
  move: () => undefined,
  canMove: () => false,
})

interface PendingReorder {
  items: ReorderItem[]
  /** Days as they were before the drop, restored on failure or cancel */
  snapshot: TripDayDetail[]
  /** Set when the activity leaves the day on screen: it disappears from view, so the toast links to its new day */
  movedTo?: { dayIndex: number; title: string }
}

interface DragDropContainerProps {
  tripId: number
  days: TripDayDetail[]
  /**
   * Renders the days; during a drag they already show the activity at its hover position.
   * moveToOtherDay goes through the same save path as a drop (menu "Chuyển sang ngày…").
   */
  children: (days: TripDayDetail[], actions: { moveToOtherDay: (activityId: number, dayId: number) => void }) => ReactNode
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
    onSuccess: (affected, { movedTo }) => {
      setConflict(null)
      // The response carries the final orderIndex, including a backend renumbering
      setCachedDays((current) => current.map((day) => affected.find((d) => d.id === day.id) ?? day))
      if (movedTo) {
        toast.success(`Đã chuyển "${movedTo.title}" sang Ngày ${movedTo.dayIndex}`, {
          label: `Mở Ngày ${movedTo.dayIndex}`,
          to: `/trips/${tripId}/days/${movedTo.dayIndex}`,
        })
      }
    },
    onError: (error, { items, snapshot, movedTo }) => {
      const apiError = getApiError(error)
      if (apiError?.errorCode === 'ACTIVITY_TIME_CONFLICT') {
        // details: 'Trùng giờ với hoạt động "..." (09:00 - 10:00)', one per moved activity that overlaps
        const messages = apiError.details.flatMap((d) => (d.message ? [d.message] : []))
        setConflict({ items, snapshot, movedTo, messages: messages.length > 0 ? messages : [apiError.message] })
        return
      }
      setConflict(null)
      setCachedDays(() => snapshot)
      toast.error(`Không sắp xếp được: ${getErrorMessage(error)}. Các hoạt động đã về chỗ cũ.`)
    },
  })

  function handleDragStart({ active }: DragStartEvent) {
    mutation.reset()
    setActiveId(parseKey(active.id).id)
    setDragDays(days)
  }

  /**
   * Hovering over another day's rail moves the activity there in the working copy. A day-list entry does not:
   * that day is not on screen, so the move happens on drop only.
   */
  function handleDragOver({ active, over }: DragOverEvent) {
    if (!over || !dragDays || parseKey(over.id).kind === 'nav') return
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
    const overTarget = parseKey(over.id)
    // Dropped on a day in the day list: to the end of that day
    if (overTarget.kind === 'nav') {
      // On the entry of the day it came from: the user changed their mind, nothing moves (the menu
      // "Chuyển sang ngày…" does not offer the current day either)
      if (findDay(days, active.id)?.id === overTarget.id) return
      const target = working.find((d) => d.id === overTarget.id)
      if (target) commitMove(moveToDay(working, movedId, target.id, target.activities.length), movedId)
      return
    }

    let finalDays = working
    const day = findDay(working, active.id)
    // Same day: dnd-kit only animated the order, apply it now
    if (day && overTarget.kind === 'activity' && overTarget.id !== movedId && findDay(working, over.id)?.id === day.id) {
      const from = day.activities.findIndex((a) => a.id === movedId)
      const to = day.activities.findIndex((a) => a.id === overTarget.id)
      finalDays = working.map((d) => (d.id === day.id ? { ...d, activities: arrayMove(d.activities, from, to) } : d))
    }

    commitMove(finalDays, movedId)
  }

  /**
   * Shared by drag and drop and the arrow buttons: show the new order at once, then send only the moved activity.
   * The overlap question and the rollback on error work the same for both.
   */
  function commitMove(finalDays: TripDayDetail[], movedId: number) {
    const target = findDay(finalDays, activityKey(movedId))
    const origin = findDay(days, activityKey(movedId))
    if (!target || !origin) return
    const unchanged =
      target.id === origin.id &&
      target.activities.findIndex((a) => a.id === movedId) === origin.activities.findIndex((a) => a.id === movedId)
    if (unchanged) return

    const items = reorderItemsForDrop(target.id, target.activities, movedId)
    const moved = origin.activities.find((a) => a.id === movedId)
    const movedTo = target.id !== origin.id && moved ? { dayIndex: target.dayIndex, title: moved.title } : undefined
    // Stop a background refetch from overwriting the optimistic view with the old order
    void queryClient.cancelQueries({ queryKey: ['trip', tripId] })
    setCachedDays(() => applyItems(finalDays, items))
    mutation.mutate({ items, snapshot: days, movedTo, allowOverlap: false })
  }

  function moveToOtherDay(activityId: number, dayId: number) {
    const target = days.find((d) => d.id === dayId)
    if (!target || target.activities.some((a) => a.id === activityId)) return
    commitMove(moveToDay(days, activityId, dayId, target.activities.length), activityId)
  }

  /** Position of an activity inside its day, and that day */
  function locate(activityId: number) {
    const day = days.find((d) => d.activities.some((a) => a.id === activityId))
    return day ? { day, index: day.activities.findIndex((a) => a.id === activityId) } : undefined
  }

  function canMove(activityId: number, direction: -1 | 1): boolean {
    const found = locate(activityId)
    if (!found) return false
    const next = found.index + direction
    return next >= 0 && next < found.day.activities.length
  }

  /** Touch screens have no drag and drop (it fights with scrolling, UI_GUIDE 11): one step per tap instead. */
  function move(activityId: number, direction: -1 | 1) {
    const found = locate(activityId)
    if (!found || !canMove(activityId, direction)) return
    const { day, index } = found
    commitMove(
      days.map((d) => (d.id === day.id ? { ...d, activities: arrayMove(d.activities, index, index + direction) } : d)),
      activityId,
    )
  }

  function handleDragCancel() {
    setDragDays(null)
    setActiveId(null)
  }

  const shownDays = dragDays ?? days
  const activeActivity =
    activeId === null ? undefined : shownDays.flatMap((d) => d.activities).find((a) => a.id === activeId)
  const reorder: ReorderContextValue = {
    disabled: mutation.isPending || conflict !== null,
    move,
    canMove,
  }

  return (
    <ReorderContext.Provider value={reorder}>
      <DndContext
        sensors={sensors}
        collisionDetection={collisionDetection}
        onDragStart={handleDragStart}
        onDragOver={handleDragOver}
        onDragEnd={handleDragEnd}
        onDragCancel={handleDragCancel}
      >
        {children(shownDays, { moveToOtherDay })}
        {/* The card following the pointer: lifted, tilted 2deg, slightly see-through (UI_GUIDE 7.3) */}
        <DragOverlay>
          {activeActivity && (
            <div className="rotate-2 rounded-card opacity-90 shadow-lg">
              <ActivityCard activity={activeActivity} dragHandle={<GripIcon />} />
            </div>
          )}
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
        onConfirm={() =>
          conflict &&
          mutation.mutate({
            items: conflict.items,
            snapshot: conflict.snapshot,
            movedTo: conflict.movedTo,
            allowOverlap: true,
          })
        }
      >
        <ul className="list-disc space-y-1 pl-5">
          {conflict?.messages.map((message) => (
            <li key={message}>{message}</li>
          ))}
        </ul>
        <p>Bạn vẫn muốn chuyển hoạt động sang ngày này? Chọn Huỷ để đưa hoạt động về chỗ cũ.</p>
      </ConfirmDialog>
    </ReorderContext.Provider>
  )
}

// Rail geometry (UI_GUIDE 7.4): a 40px time column, then the rail centred at 48px, then the card.
// The vertical line is drawn by the list; each row places its dot on it.
const ROW_GRID = 'grid grid-cols-[40px_16px_minmax(0,1fr)]'

interface SortableDayListProps {
  dayId: number
  activityIds: number[]
  children: ReactNode
}

/**
 * The rail of one day: a sortable list and a drop target, so an empty day can receive activities.
 * The 1px tide line runs through the dots; it is hidden while the day is empty.
 */
export function SortableDayList({ dayId, activityIds, children }: SortableDayListProps) {
  const { setNodeRef, isOver } = useDroppable({ id: dayKey(dayId) })
  const rail = activityIds.length > 0 ? 'before:absolute before:inset-y-3 before:left-12 before:w-px before:bg-tide' : ''
  return (
    <SortableContext id={dayKey(dayId)} items={activityIds.map(activityKey)} strategy={verticalListSortingStrategy}>
      {/* isolate: the station dots (z-10, above the rail line) stay inside the list and never paint over the
          sticky day header (BUG-UI-002) */}
      <ol
        ref={setNodeRef}
        className={`relative isolate min-h-12 space-y-3 rounded-card transition-colors ${rail} ${isOver ? 'bg-jade-light/60' : ''}`}
      >
        {children}
      </ol>
    </SortableContext>
  )
}

interface DayDropTargetProps {
  dayId: number
  children: ReactNode
}

/** An entry of the day list that accepts a dragged activity; lights up while the card is over it. */
export function DayDropTarget({ dayId, children }: DayDropTargetProps) {
  const { setNodeRef, isOver } = useDroppable({ id: navKey(dayId) })
  return (
    <div ref={setNodeRef} className={`rounded-control transition-shadow ${isOver ? 'ring-2 ring-jade ring-offset-2' : ''}`}>
      {children}
    </div>
  )
}

interface SortableActivityProps {
  activity: Activity
  /** Receives the drag handle to place inside the card; only the handle starts a drag */
  children: (dragHandle: ReactNode) => ReactNode
}

/** One "station": start time on the left, a dot in the route colour on the rail, the card on the right. */
export function SortableActivity({ activity, children }: SortableActivityProps) {
  const { disabled, move, canMove } = useContext(ReorderContext)
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: activityKey(activity.id),
    disabled,
  })

  // Mouse / pen: the grip. Touch screens: two 44px arrow buttons (UI_GUIDE 11, 12), chosen by pointer type,
  // not by screen width, so a narrow desktop window keeps drag and drop
  const handle = (
    <>
      <button
        type="button"
        ref={setActivatorNodeRef}
        {...attributes}
        {...listeners}
        aria-label={`Kéo để sắp xếp ${activity.title}`}
        className="cursor-grab touch-none rounded-control p-1 hover:bg-gray-100 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none active:cursor-grabbing aria-disabled:cursor-not-allowed aria-disabled:opacity-40 pointer-coarse:hidden"
      >
        <GripIcon />
      </button>
      <div className="hidden flex-col pointer-coarse:flex">
        <MoveButton
          label={`Chuyển ${activity.title} lên`}
          disabled={disabled || !canMove(activity.id, -1)}
          onClick={() => move(activity.id, -1)}
        >
          <ChevronUp aria-hidden className="size-5" />
        </MoveButton>
        <MoveButton
          label={`Chuyển ${activity.title} xuống`}
          disabled={disabled || !canMove(activity.id, 1)}
          onClick={() => move(activity.id, 1)}
        >
          <ChevronDown aria-hidden className="size-5" />
        </MoveButton>
      </div>
    </>
  )

  return (
    <li ref={setNodeRef} style={{ transform: CSS.Translate.toString(transform), transition }} className={ROW_GRID}>
      <span className="tabular pt-3 pr-2 text-right text-[13px] leading-4 font-semibold tracking-[0.02em] text-gray-700">
        {activity.startTime ?? <span className="font-normal text-gray-400">—</span>}
      </span>
      <span aria-hidden className="relative z-10 flex justify-center pt-3.5">
        <span className={`size-2.5 rounded-full border-[3px] bg-white ${ACTIVITY_ROUTE[activity.type].dot}`} />
      </span>
      {/* While dragging, the row keeps its height and shows where the card will land: a 2px jade line */}
      <div className="relative pl-2">
        <div className={isDragging ? 'invisible' : undefined}>{children(handle)}</div>
        {isDragging && <div aria-hidden className="absolute inset-x-2 top-1/2 h-0.5 -translate-y-1/2 rounded-full bg-jade" />}
      </div>
    </li>
  )
}

interface MoveButtonProps {
  label: string
  disabled: boolean
  onClick: () => void
  children: ReactNode
}

function MoveButton({ label, disabled, onClick, children }: MoveButtonProps) {
  return (
    <button
      type="button"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className="flex size-11 items-center justify-center rounded-control text-gray-600 hover:bg-gray-100 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none disabled:opacity-30"
    >
      {children}
    </button>
  )
}

function GripIcon() {
  return <GripVertical aria-hidden className="size-4 text-gray-400" />
}
