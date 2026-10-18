/** Share of the limit from which the counter shows (UI_GUIDE 7.2, Task 4.9 decision 5). */
const SHOW_FROM = 0.8

export interface CharCounter {
  /** "230 / 255" */
  text: string
  /** True once the limit is reached: the field takes no more characters */
  full: boolean
}

/**
 * The counter under a note field: nothing while there is plenty of room, "230 / 255" once 80% of the limit is
 * used. Counts like the field's maxLength and the server's @Size do (string length), so the number shown is the
 * one that gets checked. No limit, no counter.
 */
export function charCounter(length: number, max: number | undefined): CharCounter | null {
  if (!max || max <= 0) return null
  if (length < Math.ceil(max * SHOW_FROM)) return null
  return { text: `${length} / ${max}`, full: length >= max }
}
