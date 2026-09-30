import { useEffect, useState } from 'react'
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
 * Left (200px): list of days that jumps to a day and follows the scroll. Right: every day stacked, so an
 * activity can be dragged from one day to another without switching views (UI_GUIDE 8.1, cách a).
 * The map column comes in Phase 3.
 */
export function DayTimeline({ tripId, days, tripCurrency }: DayTimelineProps) {
  const activeDayId = useVisibleDay(days)

  return (
    <div className="grid gap-8 lg:grid-cols-[200px_minmax(0,1fr)]">
      <nav aria-label="Các ngày" className="hidden lg:block">
        <ol className="sticky top-6 max-h-[calc(100vh-3rem)] space-y-0.5 overflow-y-auto">
          {days.map((day) => {
            const active = day.id === activeDayId
            return (
              <li key={day.id}>
                <a
                  href={`#day-${day.id}`}
                  aria-current={active ? 'location' : undefined}
                  className={`relative flex items-start justify-between gap-2 rounded-control py-2 pr-2 pl-4 text-sm transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none ${
                    active
                      ? 'bg-jade-light text-jade-dark before:absolute before:inset-y-1.5 before:left-0 before:w-[3px] before:rounded-full before:bg-jade'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  <span className="min-w-0">
                    <span className="font-semibold">Ngày {day.dayIndex}</span>{' '}
                    <span className="tabular text-gray-500">{formatDate(day.date).slice(0, 5)}</span>
                    {day.title ? (
                      <span className="block truncate text-xs text-gray-500">{day.title}</span>
                    ) : (
                      <span className="block text-xs text-gray-400 italic">Chưa có tiêu đề</span>
                    )}
                  </span>
                  <span className="tabular mt-0.5 shrink-0 rounded-control bg-white/70 px-1.5 text-xs text-gray-600">
                    {day.activities.length}
                  </span>
                </a>
              </li>
            )
          })}
        </ol>
      </nav>
      <div className="min-w-0">
        <DragDropContainer tripId={tripId} days={days}>
          {(shownDays) => (
            <div className="divide-y divide-tide">
              {shownDays.map((day) => (
                <DaySection key={day.id} tripId={tripId} day={day} tripCurrency={tripCurrency} />
              ))}
            </div>
          )}
        </DragDropContainer>
      </div>
    </div>
  )
}

/**
 * The day whose section is nearest the top of the screen (scroll-spy). Sections are watched while they cross a
 * band at the top 30% of the viewport; the first one in that band wins, the first day before any scroll.
 */
function useVisibleDay(days: TripDayDetail[]): number | undefined {
  const [visibleId, setVisibleId] = useState<number>()
  const dayKey = days.map((d) => d.id).join(',')

  useEffect(() => {
    const inBand = new Map<number, number>()
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const id = Number(entry.target.id.slice('day-'.length))
          if (entry.isIntersecting) inBand.set(id, entry.boundingClientRect.top)
          else inBand.delete(id)
        }
        const first = [...inBand.entries()].sort((a, b) => a[1] - b[1])[0]
        if (first) setVisibleId(first[0])
      },
      { rootMargin: '0px 0px -70% 0px' },
    )
    for (const id of dayKey.split(',')) {
      const section = document.getElementById(`day-${id}`)
      if (section) observer.observe(section)
    }
    return () => observer.disconnect()
  }, [dayKey])

  return visibleId ?? days[0]?.id
}
