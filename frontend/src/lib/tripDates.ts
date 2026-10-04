import { countDays } from './format'
import type { TripStatus } from '../types/trip'

/**
 * How a date edit affects the days of a trip (design.md rule 14.3):
 * - shift: same length, new start → every day and activity moves along, nothing is lost
 * - resize: same start, new end → days outside the new range are deleted
 * - shift-and-resize: the backend keeps the days whose date is still inside the new range, deletes the rest
 */
export type DateChange = 'none' | 'shift' | 'resize' | 'shift-and-resize'

export function classifyDateChange(
  oldStart: string,
  oldEnd: string,
  newStart: string,
  newEnd: string,
): DateChange {
  const startChanged = newStart !== oldStart
  const lengthChanged = countDays(newStart, newEnd) !== countDays(oldStart, oldEnd)
  if (startChanged && lengthChanged) return 'shift-and-resize'
  if (startChanged) return 'shift'
  if (newEnd !== oldEnd) return 'resize'
  return 'none'
}

interface DatedDay {
  dayIndex: number
  /** "YYYY-MM-DD" */
  date: string
}

/**
 * The day to show when a trip is opened without naming one (/trips/:id): today's day while the trip is in
 * progress, day 1 before it starts and after it is over (design.md rule 14.22). "In progress" goes by the
 * dates, not by the status, which only changes when the user changes it.
 */
export function openingDayIndex(days: readonly DatedDay[], today: string): number {
  return days.find((day) => day.date === today)?.dayIndex ?? 1
}

/** Statuses of a trip that has not been closed by its owner yet */
const OPEN_STATUSES: readonly TripStatus[] = ['DRAFT', 'PLANNED', 'ONGOING']

/**
 * True when the trip is over by its dates but its status does not say so: the moment to ask the user whether
 * it is completed (design.md rule 14.22). The status never changes by itself, and a trip the user already
 * completed or archived is not asked about. A trip is over once its last day is before today.
 */
export function shouldAskToComplete(trip: { endDate: string; status: TripStatus }, today: string): boolean {
  return trip.endDate < today && OPEN_STATUSES.includes(trip.status)
}
