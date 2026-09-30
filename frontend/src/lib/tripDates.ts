import { countDays } from './format'

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
