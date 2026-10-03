import { lazy, Suspense, useEffect, useRef, useState, type ReactNode, type Ref, type RefObject } from 'react'
import { Map as MapIcon, Maximize2, X } from 'lucide-react'
import { Skeleton } from '../../components/Skeleton'
import { revealActivity } from '../../stores/mapLinkStore'
import type { Activity, ActivityType } from '../../types/activity'
import type { Coordinates } from '../../types/place'
import type { RevealMode } from './DayMapCanvas'

// The map library is large and only this page needs it: it is fetched when a trip is opened, not with the app
const DayMapCanvas = lazy(() => import('./DayMapCanvas'))

/** One marker of the day: an activity that has a place. */
export interface DayStop {
  activityId: number
  /** 1, 2, 3... among the activities that have a place, in the order the day shows them */
  number: number
  title: string
  type: ActivityType
  lat: number
  lng: number
}

/** Activities without a place are not on the map and take no number. */
function toStops(activities: Activity[]): DayStop[] {
  return activities.flatMap((activity) => (activity.place ? [{ activity, place: activity.place }] : []))
    .map(({ activity, place }, index) => ({
      activityId: activity.id,
      number: index + 1,
      title: activity.title,
      type: activity.type,
      lat: place.lat,
      lng: place.lng,
    }))
}

interface DayMapProps {
  /** Activities of the day being shown, in display order */
  activities: Activity[]
  /** Position of the trip's destination; null when the trip has none */
  destination: Coordinates | null
  /**
   * Narrow screens show either the list or the map: while the list is hidden its cards cannot be scrolled to.
   * A marker then opens the name box, and "Xem trong lịch trình" asks the page to bring the list back first.
   */
  listHidden?: boolean
  onShowInList?: (activityId: number) => void
}

/**
 * The map of one day (UI_GUIDE 9): a marker for every activity that has a place. A day without any place
 * shows the area of the trip's destination and a note saying how to get markers.
 * <p>
 * "Phóng to bản đồ" opens the same map over the whole window. That view is a second map inside a native
 * dialog, not this one stretched: the dialog lives in the browser's top layer, above the pinned headers of the
 * page whatever their z-index, and brings the focus trap and the Esc key with it.
 */
export function DayMap({ activities, destination, listHidden = false, onShowInList }: DayMapProps) {
  const stops = toStops(activities)
  const [expanded, setExpanded] = useState(false)
  const expandButtonRef = useRef<HTMLButtonElement>(null)
  const dialogRef = useRef<HTMLDialogElement>(null)
  // The card asked for from the expanded map, shown once that map is closed
  const revealAfterCollapse = useRef<number | null>(null)
  // A finger cannot hover: on touch screens a tap opens the name box instead of jumping to the card
  const [touchScreen] = useState(() => window.matchMedia('(pointer: coarse)').matches)

  /**
   * The dialog is shown here, before React mounts the map inside it: the map library measures its box once,
   * when it is created, and inside a dialog that is still closed that box is 0 by 0 (BUG-UI-009).
   */
  function expand() {
    dialogRef.current?.showModal()
    setExpanded(true)
  }

  function collapse() {
    setExpanded(false)
    // Back to where the user was, like closing any dialog
    expandButtonRef.current?.focus()
  }

  /** To the card of the activity, through the page when the list has to be shown first. */
  function goToCard(activityId: number) {
    if (listHidden && onShowInList) onShowInList(activityId)
    else revealActivity(activityId)
  }

  // While the expanded map is open the page behind it cannot take the focus. This effect runs after the one
  // of ExpandedMap (children first), which has closed the dialog by then. goToCard is read at that moment, not
  // listed as a dependency: the effect must run when the dialog closes, not when the list is shown or hidden
  const goToCardRef = useRef(goToCard)
  useEffect(() => {
    goToCardRef.current = goToCard
  })
  useEffect(() => {
    if (expanded || revealAfterCollapse.current === null) return
    goToCardRef.current(revealAfterCollapse.current)
    revealAfterCollapse.current = null
  }, [expanded])

  return (
    <>
      <MapFrame
        stops={stops}
        destination={destination}
        className="h-full rounded-card border border-tide"
        revealMode={touchScreen || listHidden ? 'popup' : 'direct'}
        onReveal={goToCard}
        action={
          <MapButton ref={expandButtonRef} label="Phóng to bản đồ" onClick={expand}>
            <Maximize2 aria-hidden className="size-4" />
          </MapButton>
        }
      />
      <ExpandedMap ref={dialogRef} open={expanded} onClose={collapse}>
        <MapFrame
          stops={stops}
          destination={destination}
          className="h-full"
          revealMode="popup"
          onReveal={(activityId) => {
            revealAfterCollapse.current = activityId
            setExpanded(false)
          }}
          action={
            <MapButton label="Thu nhỏ bản đồ" onClick={collapse}>
              <X aria-hidden className="size-4" />
            </MapButton>
          }
        />
      </ExpandedMap>
    </>
  )
}

interface MapFrameProps {
  stops: DayStop[]
  destination: Coordinates | null
  /** Size and border of the frame; the inside is the same wherever the map is shown */
  className: string
  /** Button in the top right corner: expand, or close when already expanded */
  action: ReactNode
  revealMode: RevealMode
  onReveal: (activityId: number) => void
}

/**
 * The map with what sits on top of it. The frame is its own stacking context: the map library stacks its
 * layers up to z-index 1000, and without "isolate" they would cover the sticky day header and the dialogs'
 * backdrop (BUG-UI-002).
 */
function MapFrame({ stops, destination, className, action, revealMode, onReveal }: MapFrameProps) {
  return (
    <section aria-label="Bản đồ của ngày" className={`relative isolate overflow-hidden bg-gray-100 ${className}`}>
      {/* z-0 closes the layers of the map library into their own stack, so the note and the button sit above */}
      <div className="relative z-0 h-full">
        <Suspense fallback={<Skeleton className="h-full" />}>
          <DayMapCanvas stops={stops} destination={destination} revealMode={revealMode} onReveal={onReveal} />
        </Suspense>
      </div>
      <div className="absolute top-3 right-3 z-10">{action}</div>
      {stops.length === 0 && (
        // The wrapper lets the pointer through: around the note the map can still be dragged and zoomed
        <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center p-6">
          <div className="pointer-events-auto max-w-[280px] rounded-card border border-tide bg-white px-5 py-4 text-center shadow-md">
            <MapIcon aria-hidden className="mx-auto size-5 text-gray-400" />
            <p className="mt-2 text-[15px] leading-5 font-semibold text-ink">Ngày này chưa có địa điểm nào.</p>
            <p className="mt-1 text-[13px] leading-5 text-gray-600">
              Thêm địa điểm cho hoạt động để thấy trên bản đồ.
              {!destination && ' Đặt vị trí điểm đến trong "Sửa" chuyến đi để bản đồ mở đúng nơi bạn đến.'}
            </p>
          </div>
        </div>
      )}
    </section>
  )
}

interface MapButtonProps {
  label: string
  onClick: () => void
  children: ReactNode
  ref?: Ref<HTMLButtonElement>
}

/** White square button over the map: 36px with a mouse, 44px on touch screens. */
function MapButton({ label, onClick, children, ref }: MapButtonProps) {
  return (
    <button
      ref={ref}
      type="button"
      aria-label={label}
      title={label}
      onClick={onClick}
      className="flex size-9 items-center justify-center rounded-control border border-tide bg-white text-gray-700 shadow-sm transition-colors hover:bg-gray-50 hover:text-ink focus-visible:ring-[3px] focus-visible:ring-jade/40 focus-visible:outline-none pointer-coarse:size-11"
    >
      {children}
    </button>
  )
}

interface ExpandedMapProps {
  /** The caller shows the dialog itself (showModal) before setting "open": see expand() */
  ref: RefObject<HTMLDialogElement | null>
  open: boolean
  onClose: () => void
  children: ReactNode
}

/**
 * A dialog as large as the window, without the title bar and padding of Modal. The children mount only while
 * it is open, and it is already on screen by then: opening it in an effect would be too late, because the
 * effects and refs of the children (the map) run before those of this component.
 */
function ExpandedMap({ ref, open, onClose, children }: ExpandedMapProps) {
  useEffect(() => {
    const dialog = ref.current
    if (!open && dialog?.open) dialog.close()
  }, [ref, open])

  return (
    <dialog
      ref={ref}
      aria-label="Bản đồ của ngày, toàn màn hình"
      // Esc: go through the same path as the button, so the focus returns to "Phóng to bản đồ"
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
      // 100% of the window, not 100vw: that unit includes the scrollbar and would push the close button under it
      className="m-0 h-full max-h-none w-full max-w-none border-0 bg-paper p-0"
    >
      {open && children}
    </dialog>
  )
}
