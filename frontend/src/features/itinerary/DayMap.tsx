import { lazy, Suspense, useEffect, useRef, useState, type ReactNode, type Ref } from 'react'
import { Map as MapIcon, Maximize2, X } from 'lucide-react'
import { Skeleton } from '../../components/Skeleton'
import type { Activity, ActivityType } from '../../types/activity'
import type { Coordinates } from '../../types/place'

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
}

/**
 * The map of one day (UI_GUIDE 9): a marker for every activity that has a place. A day without any place
 * shows the area of the trip's destination and a note saying how to get markers.
 * <p>
 * "Phóng to bản đồ" opens the same map over the whole window. That view is a second map inside a native
 * dialog, not this one stretched: the dialog lives in the browser's top layer, above the pinned headers of the
 * page whatever their z-index, and brings the focus trap and the Esc key with it.
 */
export function DayMap({ activities, destination }: DayMapProps) {
  const stops = toStops(activities)
  const [expanded, setExpanded] = useState(false)
  const expandButtonRef = useRef<HTMLButtonElement>(null)

  function collapse() {
    setExpanded(false)
    // Back to where the user was, like closing any dialog
    expandButtonRef.current?.focus()
  }

  return (
    <>
      <MapFrame
        stops={stops}
        destination={destination}
        className="h-full rounded-card border border-tide"
        action={
          <MapButton ref={expandButtonRef} label="Phóng to bản đồ" onClick={() => setExpanded(true)}>
            <Maximize2 aria-hidden className="size-4" />
          </MapButton>
        }
      />
      <ExpandedMap open={expanded} onClose={collapse}>
        <MapFrame
          stops={stops}
          destination={destination}
          className="h-full"
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
}

/**
 * The map with what sits on top of it. The frame is its own stacking context: the map library stacks its
 * layers up to z-index 1000, and without "isolate" they would cover the sticky day header and the dialogs'
 * backdrop (BUG-UI-002).
 */
function MapFrame({ stops, destination, className, action }: MapFrameProps) {
  return (
    <section aria-label="Bản đồ của ngày" className={`relative isolate overflow-hidden bg-gray-100 ${className}`}>
      {/* z-0 closes the layers of the map library into their own stack, so the note and the button sit above */}
      <div className="relative z-0 h-full">
        <Suspense fallback={<Skeleton className="h-full" />}>
          <DayMapCanvas stops={stops} destination={destination} />
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
  open: boolean
  onClose: () => void
  children: ReactNode
}

/**
 * A dialog as large as the window, without the title bar and padding of Modal. The children mount only while
 * it is open: the map inside measures its box when it is created, and a closed dialog has none.
 */
function ExpandedMap({ open, onClose, children }: ExpandedMapProps) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

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
