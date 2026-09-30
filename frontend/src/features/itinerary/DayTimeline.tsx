import { formatDate } from '../../lib/format'
import type { TripDayDetail } from '../../types/trip'
import { DaySection } from './DaySection'
import { DragDropContainer } from './DragDropContainer'

interface DayTimelineProps {
  tripId: number
  days: TripDayDetail[]
  tripCurrency: string
}

/**
 * Left: list of days that scrolls to a day. Right: every day stacked, so an activity can be dragged from one
 * day to another without switching views. The map column comes in Phase 3.
 */
export function DayTimeline({ tripId, days, tripCurrency }: DayTimelineProps) {
  return (
    <div className="grid gap-6 lg:grid-cols-[14rem_1fr]">
      <nav aria-label="Các ngày" className="hidden lg:block">
        <ol className="sticky top-4 max-h-[calc(100vh-2rem)] space-y-1 overflow-y-auto">
          {days.map((day) => (
            <li key={day.id}>
              <a
                href={`#day-${day.id}`}
                className="flex items-center justify-between gap-2 rounded-control px-3 py-2 text-sm text-gray-700 hover:bg-white"
              >
                <span className="truncate">
                  <span className="font-medium">Ngày {day.dayIndex}</span> · {formatDate(day.date).slice(0, 5)}
                  {day.title ? (
                    <span className="block truncate text-xs text-gray-500">{day.title}</span>
                  ) : (
                    <span className="block text-xs italic text-gray-400">Chưa có tiêu đề</span>
                  )}
                </span>
                <span className="shrink-0 rounded-full bg-gray-200 px-2 text-xs text-gray-600">
                  {day.activities.length}
                </span>
              </a>
            </li>
          ))}
        </ol>
      </nav>
      <div className="min-w-0 space-y-4">
        <DragDropContainer tripId={tripId} days={days}>
          {(shownDays) =>
            shownDays.map((day) => <DaySection key={day.id} tripId={tripId} day={day} tripCurrency={tripCurrency} />)
          }
        </DragDropContainer>
      </div>
    </div>
  )
}
