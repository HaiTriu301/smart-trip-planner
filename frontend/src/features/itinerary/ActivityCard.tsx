import type { ReactNode } from 'react'
import { ExternalLink, MapPin, TriangleAlert, Wallet } from 'lucide-react'
import { ExpandableText } from '../../components/ExpandableText'
import { formatMoney } from '../../lib/format'
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
 * The most important component (UI_GUIDE 7.3): white card, 1px tide border, 3px left edge in the route colour.
 * First row: time range + type (icon and label, so colour is never the only signal) + "⋮" menu. Under the
 * title, the place of the activity with a pin icon.
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

  return (
    <article
      className={`group flex gap-1 rounded-card border-y border-r border-l-[3px] border-y-tide border-r-tide py-3 pr-2 pl-1 transition-colors ${route.edge} ${
        overlapping ? 'bg-warning/8' : 'bg-white hover:bg-gray-50'
      }`}
    >
      {dragHandle && <div className={`shrink-0 self-start ${REVEAL}`}>{dragHandle}</div>}

      <div className={`min-w-0 flex-1 space-y-1 ${dragHandle ? '' : 'pl-2'}`}>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[13px] leading-5">
          <span className="tabular inline-flex items-center gap-1 font-semibold text-gray-700">
            {overlapping && (
              <>
                <TriangleAlert aria-hidden className="size-3.5 text-warning" />
                <span className="sr-only">Trùng giờ với hoạt động khác, </span>
              </>
            )}
            {time ?? <span className="font-normal text-gray-400">Chưa đặt giờ</span>}
          </span>
          <span className={`inline-flex items-center gap-1 font-medium ${route.text}`}>
            <route.Icon aria-hidden className="size-3.5" />
            {ACTIVITY_TYPE_LABELS[activity.type]}
          </span>
        </div>
        <h4 className="text-base leading-6 font-semibold wrap-anywhere text-ink">{activity.title}</h4>
        {place && (
          <p className="flex items-start gap-1 text-[13px] leading-5 text-gray-600">
            <MapPin aria-hidden className="mt-[3px] size-3.5 shrink-0 text-gray-400" />
            <span className="sr-only">Địa điểm: </span>
            <span className="wrap-anywhere">{place}</span>
          </p>
        )}
        {activity.note && <ExpandableText text={activity.note} className="max-w-[68ch] text-[13px] leading-5 text-gray-600" />}
        {(activity.costAmount !== null || activity.bookingUrl) && (
          <div className="flex flex-wrap gap-x-4 gap-y-1 pt-0.5 text-xs text-gray-600">
            {activity.costAmount !== null && activity.currency && (
              <span className="tabular inline-flex items-center gap-1">
                <Wallet aria-hidden className="size-3.5 text-gray-400" />
                {formatMoney(activity.costAmount, activity.currency)}
              </span>
            )}
            {activity.bookingUrl && (
              <a
                href={activity.bookingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 font-medium text-jade hover:underline"
              >
                Link đặt chỗ
                <ExternalLink aria-hidden className="size-3" />
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
