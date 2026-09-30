import { formatMoney } from '../../lib/format'
import type { Activity } from '../../types/activity'
import { ACTIVITY_TYPE_LABELS, ACTIVITY_TYPE_STYLES } from './activityType'

/** "09:00 – 10:30", "09:00" when only the start is set, or null for an untimed activity. */
function formatTimeRange(activity: Activity): string | null {
  if (!activity.startTime) return null
  return activity.endTime ? `${activity.startTime} – ${activity.endTime}` : activity.startTime
}

export function ActivityCard({ activity }: { activity: Activity }) {
  const time = formatTimeRange(activity)

  return (
    <article className="flex gap-4 rounded-lg border border-slate-200 bg-white p-3">
      <div className="w-24 shrink-0 text-sm font-medium text-slate-700">
        {time ?? <span className="text-slate-400">Chưa đặt giờ</span>}
      </div>
      <div className="min-w-0 flex-1 space-y-1">
        <div className="flex flex-wrap items-center gap-2">
          <h4 className="font-medium text-slate-800">{activity.title}</h4>
          <span className={`rounded-full px-2 py-0.5 text-xs ${ACTIVITY_TYPE_STYLES[activity.type]}`}>
            {ACTIVITY_TYPE_LABELS[activity.type]}
          </span>
        </div>
        {activity.note && <p className="whitespace-pre-line text-sm text-slate-600">{activity.note}</p>}
        <div className="flex flex-wrap gap-x-4 text-sm text-slate-500">
          {activity.costAmount !== null && activity.currency && (
            <span>{formatMoney(activity.costAmount, activity.currency)}</span>
          )}
          {activity.bookingUrl && (
            <a
              href={activity.bookingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sky-700 hover:underline"
            >
              Link đặt chỗ
            </a>
          )}
        </div>
      </div>
    </article>
  )
}
