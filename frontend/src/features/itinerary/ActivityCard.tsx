import type { ReactNode } from 'react'
import { ExternalLink, MapPin, TriangleAlert } from 'lucide-react'
import { ExpandableText } from '../../components/ExpandableText'
import { formatMoney } from '../../lib/format'
import { activityCardId, useMapLinkStore } from '../../stores/mapLinkStore'
import type { Activity } from '../../types/activity'
import { ActivityMenu } from './ActivityMenu'
import { ACTIVITY_ROUTE, ACTIVITY_TYPE_LABELS } from './activityType'

/** "09:00 – 10:30", "09:00" when only the start is set, or null for an untimed activity. */
function formatTimeRange(activity: Activity): string | null {
  if (!activity.startTime) return null
  return activity.endTime ? `${activity.startTime} – ${activity.endTime}` : activity.startTime
}

/**
 * The place row: "Chợ Hàn · Phường Hải Châu, Đà Nẵng". The name is left out when the title already says it
 * (an activity is often named after its place) and an address follows; null when no place is attached.
 */
function formatPlace(activity: Activity): string | null {
  const place = activity.place
  if (!place) return null
  const sameAsTitle = place.name.trim().toLowerCase() === activity.title.trim().toLowerCase()
  if (!place.address) return place.name
  return sameAsTitle ? place.address : `${place.name} · ${place.address}`
}

// Shown on hover (and keyboard focus) with a mouse; always shown on touch screens, which have no hover
const REVEAL = 'opacity-0 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100 pointer-coarse:opacity-100'

interface ActivityCardProps {
  activity: Activity
  /** Grip at the left edge; the only place a drag can start */
  dragHandle?: ReactNode
  /** Its time range overlaps another activity of the day (saved with allowOverlap) */
  overlapping?: boolean
  /** Without handlers the card is read-only (the preview that follows the pointer while dragging) */
  onEdit?: () => void
  onDelete?: () => void
  onMoveToDay?: () => void
}

/**
 * The most important component (UI_GUIDE 7.3): a white card with a soft shadow and no border. First row: the
 * time range, the type as a tinted chip (icon and label, so colour is never the only signal) and the "⋮" menu.
 * Then the title, the place with a pin icon, the note, and a footer under a rule with the cost on the left and
 * the booking link on the right.
 */
export function ActivityCard({
  activity,
  dragHandle,
  overlapping = false,
  onEdit,
  onDelete,
  onMoveToDay,
}: ActivityCardProps) {
  const time = formatTimeRange(activity)
  const route = ACTIVITY_ROUTE[activity.type]
  const place = formatPlace(activity)
  // Only the two actions are read: the card itself does not redraw when the highlight moves
  const highlight = useMapLinkStore((state) => state.highlight)
  const clearHighlight = useMapLinkStore((state) => state.clearHighlight)
  const revealed = useMapLinkStore((state) => state.revealedActivityId === activity.id)
  // The card that follows the pointer during a drag is a second copy of the same activity: only the real one
  // (it has actions) carries the id a marker scrolls to
  const isRealCard = Boolean(onEdit)

  return (
    // An overlapping card is tinted with the warning colour mixed into white, not laid over the page: a card
    // with a shadow has to stay opaque
    <article
      id={isRealCard ? activityCardId(activity.id) : undefined}
      // Reachable by script only (a marker moves the focus here), not by Tab
      tabIndex={isRealCard ? -1 : undefined}
      // Pointer on the card, or keyboard focus inside it: its marker on the map stands out (UI_GUIDE 9)
      onMouseEnter={() => highlight(activity.id)}
      onMouseLeave={() => clearHighlight(activity.id)}
      onFocus={() => highlight(activity.id)}
      onBlur={() => clearHighlight(activity.id)}
      className={`group flex gap-1 rounded-card py-4 pr-3 pl-1.5 shadow-md transition-[color,background-color,box-shadow] focus:outline-none ${
        overlapping ? 'bg-[color-mix(in_srgb,var(--color-warning)_10%,white)]' : 'bg-white'
      } ${revealed ? 'ring-2 ring-jade/40' : ''}`}
    >
      {dragHandle && <div className={`shrink-0 self-start ${REVEAL}`}>{dragHandle}</div>}

      <div className={`min-w-0 flex-1 space-y-1.5 ${dragHandle ? '' : 'pl-2.5'}`}>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
          <span className="tabular inline-flex items-center gap-1 text-[15px] leading-6 font-semibold text-ink">
            {overlapping && (
              <>
                <TriangleAlert aria-hidden className="size-3.5 text-warning" />
                <span className="sr-only">Trùng giờ với hoạt động khác, </span>
              </>
            )}
            {time ?? <span className="text-sm font-normal text-gray-400">Chưa đặt giờ</span>}
          </span>
          <span
            className={`inline-flex h-5 items-center gap-1 rounded-full px-2 text-xs leading-none font-medium ${route.chip}`}
          >
            <route.Icon aria-hidden className="size-3" />
            {ACTIVITY_TYPE_LABELS[activity.type]}
          </span>
        </div>
        <h4 className="text-lg leading-6 font-semibold wrap-anywhere text-ink">{activity.title}</h4>
        {place && (
          <p className="flex items-start gap-1 text-[13px] leading-5 text-gray-600">
            <MapPin aria-hidden className="mt-[3px] size-3.5 shrink-0 text-gray-400" />
            <span className="sr-only">Địa điểm: </span>
            <span className="wrap-anywhere">{place}</span>
          </p>
        )}
        {activity.note && <ExpandableText text={activity.note} className="max-w-[68ch] text-[13px] leading-5 text-gray-600" />}
        {(activity.costAmount !== null || activity.bookingUrl) && (
          // Footer under a rule: what it costs on the left, where to book on the right
          <div className="mt-1.5 flex flex-wrap items-center justify-between gap-x-4 gap-y-1 border-t border-tide pt-2.5 text-[13px] leading-5">
            {activity.costAmount !== null && activity.currency ? (
              <span className="text-gray-500">
                Chi phí dự kiến:{' '}
                <span className="tabular font-semibold text-jade-dark">
                  {formatMoney(activity.costAmount, activity.currency)}
                </span>
              </span>
            ) : (
              <span />
            )}
            {activity.bookingUrl && (
              <a
                href={activity.bookingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 rounded-control font-medium text-jade-dark hover:underline focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
              >
                Link đặt chỗ
                <ExternalLink aria-hidden className="size-3.5" />
              </a>
            )}
          </div>
        )}
      </div>

      {onEdit && onDelete && (
        <ActivityMenu
          title={activity.title}
          onEdit={onEdit}
          onDelete={onDelete}
          onMoveToDay={onMoveToDay}
          className={`shrink-0 self-start ${REVEAL}`}
        />
      )}
    </article>
  )
}
