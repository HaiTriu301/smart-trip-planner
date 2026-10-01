import { useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { BackToTopButton } from '../../components/BackToTopButton'
import { LinkButton } from '../../components/LinkButton'
import { formatDate } from '../../lib/format'
import type { TripDayDetail } from '../../types/trip'
import { DaySection } from './DaySection'
import { DayDropTarget, DragDropContainer } from './DragDropContainer'

interface DayTimelineProps {
  tripId: number
  days: TripDayDetail[]
  /** The day on the URL (/trips/:id/days/:dayIndex), already checked to exist */
  currentDayIndex: number
  tripCurrency: string
}

const dayPath = (tripId: number, dayIndex: number) => `/trips/${tripId}/days/${dayIndex}`

/**
 * One day per page (UI_GUIDE 8.1): the list of days on the left (chips on narrow screens) links to each day,
 * the middle column shows only the chosen one. The whole grid sits inside the drag and drop area, so a card can
 * be dropped on another day of the list to move it there. The map column comes in Phase 3.
 * <p>
 * Long days (UI_GUIDE 8.1 "Ngày dài"): on wide screens the day list and the day header both stay pinned at the
 * same height while the activities scroll; "Đầu ngày" sits in the day header and a small floating button in the
 * bottom right corner goes back to the top of the page. On narrow screens the floating button goes back to the
 * start of the day instead.
 */
export function DayTimeline({ tripId, days, currentDayIndex, tripCurrency }: DayTimelineProps) {
  const current = days.find((d) => d.dayIndex === currentDayIndex)
  const previous = days.find((d) => d.dayIndex === currentDayIndex - 1)
  const next = days.find((d) => d.dayIndex === currentDayIndex + 1)

  // Coming from "Ngày sau" at the bottom of a long day: bring the start of the new day back into view. When the
  // start of the day is still in sight (a click on the day list near the top), the page stays where it is.
  // The section, not the heading: on wide screens the heading is pinned and counts as always visible.
  useEffect(() => {
    const start = document.getElementById('day-start')
    if (start && start.getBoundingClientRect().top < parseFloat(getComputedStyle(start).scrollMarginTop)) {
      start.scrollIntoView({ block: 'start' })
    }
  }, [currentDayIndex])

  if (!current) return null

  return (
    <DragDropContainer tripId={tripId} days={days}>
      {(shownDays, { moveToOtherDay }) => {
        // While dragging inside the day, the working copy is shown; pick the current day from it
        const shown = shownDays.find((d) => d.id === current.id) ?? current
        return (
          <div className="grid gap-x-8 gap-y-4 lg:grid-cols-[200px_minmax(0,1fr)]">
            {/* Fixed position, so they take no room in the grid. Narrow screens: back to the start of the day, the
                place that matters there. Wide screens: back to the top of the page ("Đầu ngày" is in the header) */}
            <BackToTopButton placement="floating" screens="narrow" label="Về đầu ngày" targetId="day-start" />
            <BackToTopButton placement="floating" screens="wide" label="Lên đầu trang" />
            <DayChips tripId={tripId} days={days} currentDayIndex={currentDayIndex} />
            <nav aria-label="Các ngày" className="hidden lg:block">
              <ol className="sticky top-6 max-h-[calc(100vh-3rem)] space-y-0.5 overflow-y-auto p-1">
                {days.map((day) => {
                  const active = day.dayIndex === currentDayIndex
                  return (
                    <li key={day.id}>
                      <DayDropTarget dayId={day.id}>
                        <Link
                          to={dayPath(tripId, day.dayIndex)}
                          aria-current={active ? 'page' : undefined}
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
                        </Link>
                      </DayDropTarget>
                    </li>
                  )
                })}
              </ol>
            </nav>
            <div className="min-w-0 space-y-8">
              <DaySection
                key={shown.id}
                tripId={tripId}
                day={shown}
                days={days}
                tripCurrency={tripCurrency}
                onMoveToDay={moveToOtherDay}
              />

              {(previous || next) && (
                <nav aria-label="Chuyển ngày" className="flex justify-between gap-3 border-t border-tide pt-4">
                  {previous ? (
                    <LinkButton variant="secondary" to={dayPath(tripId, previous.dayIndex)}>
                      <ChevronLeft aria-hidden className="size-4" />
                      Ngày {previous.dayIndex}
                    </LinkButton>
                  ) : (
                    <span />
                  )}
                  {next && (
                    <LinkButton variant="secondary" to={dayPath(tripId, next.dayIndex)}>
                      Ngày {next.dayIndex}
                      <ChevronRight aria-hidden className="size-4" />
                    </LinkButton>
                  )}
                </nav>
              )}
            </div>
          </div>
        )
      }}
    </DragDropContainer>
  )
}

interface DayChipsProps {
  tripId: number
  days: TripDayDetail[]
  currentDayIndex: number
}

/** Phones and tablets: the day list as chips that scroll sideways, stuck to the top of the screen. */
function DayChips({ tripId, days, currentDayIndex }: DayChipsProps) {
  const activeRef = useRef<HTMLAnchorElement>(null)

  // Keep the chip of the current day visible in the row
  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  }, [currentDayIndex])

  return (
    <nav
      aria-label="Các ngày"
      className="sticky top-0 z-20 -mx-4 border-b border-tide bg-paper px-4 py-2 sm:-mx-6 sm:px-6 lg:hidden"
    >
      <ol className="flex gap-2 overflow-x-auto">
        {days.map((day) => {
          const active = day.dayIndex === currentDayIndex
          return (
            <li key={day.id} className="shrink-0">
              <Link
                ref={active ? activeRef : undefined}
                to={dayPath(tripId, day.dayIndex)}
                aria-current={active ? 'page' : undefined}
                className={`tabular inline-flex h-9 items-center rounded-control px-3 text-[13px] font-medium whitespace-nowrap transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none ${
                  active ? 'bg-ink text-white' : 'border border-tide bg-white text-gray-600'
                }`}
              >
                Ngày {day.dayIndex} · {formatDate(day.date).slice(0, 5)}
              </Link>
            </li>
          )
        })}
      </ol>
    </nav>
  )
}
