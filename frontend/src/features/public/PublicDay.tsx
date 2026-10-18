import { Flag } from 'lucide-react'
import { formatDate, formatWeekday } from '../../lib/format'
import type { PublicDay as PublicDayData } from '../../types/publicTrip'
import { ActivityCardContent } from '../itinerary/ActivityCard'
import { ACTIVITY_ROUTE } from '../itinerary/activityType'

/**
 * One day of the public trip page (UI_GUIDE 8.5): "Ngày 1 · Thứ năm, 12/03", the day's title under it, then the
 * timeline rail of the app (UI_GUIDE 7.4) with read-only cards: the same card body as the itinerary, without the
 * grip, the "⋮" menu or any add button. No cost and no booking link: the server does not send them to guests.
 */
export function PublicDay({ day }: { day: PublicDayData }) {
  const headingId = `public-day-${day.dayIndex}`
  return (
    <section aria-labelledby={headingId} className="space-y-3">
      <div>
        <h2 id={headingId} className="tabular text-lg leading-[26px] font-semibold text-ink">
          Ngày {day.dayIndex} · {formatWeekday(day.date)}, {formatDate(day.date).slice(0, 5)}
        </h2>
        {day.title && (
          <p className="mt-0.5 flex items-start gap-2 font-medium text-jade-dark">
            <Flag aria-hidden className="mt-1 size-4 shrink-0" />
            <span className="min-w-0 wrap-anywhere">{day.title}</span>
          </p>
        )}
      </div>
      {day.activities.length === 0 ? (
        <p className="rounded-card border border-dashed border-gray-300 px-4 py-5 text-sm text-gray-600">
          Ngày này chưa có hoạt động.
        </p>
      ) : (
        // The rail of the itinerary: a 2px line through one node per activity, in the colour of its type.
        // isolate keeps the nodes (z-10) inside the list (BUG-UI-002)
        <ol className="relative isolate space-y-4 before:absolute before:inset-y-6 before:left-[5px] before:w-0.5 before:bg-tide">
          {day.activities.map((activity, index) => (
            // The server sends no id: the position in the day is the identity, and the list never reorders
            <li key={index} className="grid grid-cols-[12px_minmax(0,1fr)]">
              <span aria-hidden className="relative z-10 flex justify-center pt-[23px]">
                <span className={`size-2.5 rounded-full border-2 bg-white ${ACTIVITY_ROUTE[activity.type].dot}`} />
              </span>
              <div className="pl-1.5">
                <article className="rounded-card bg-white py-4 pr-3 pl-4 shadow-md">
                  <ActivityCardContent activity={activity} />
                </article>
              </div>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
