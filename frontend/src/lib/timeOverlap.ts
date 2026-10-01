// Overlapping activities of one day, for a soft warning on the cards (UI_GUIDE 7.3). Same rule as the backend
// (design.md rule 14.4): only activities with both times count, and touching ends (09:00–10:00, 10:00–11:00) do not
// overlap. The server stays authoritative; overlaps saved with allowOverlap=true are the ones shown here.

interface Timed {
  id: number
  startTime: string | null
  endTime: string | null
}

/** ids of the activities whose time range overlaps another activity of the same list. "HH:mm" compares as text. */
export function findOverlaps(activities: readonly Timed[]): Set<number> {
  const timed = activities.flatMap((a) => (a.startTime && a.endTime ? [{ id: a.id, start: a.startTime, end: a.endTime }] : []))
  const overlapping = new Set<number>()
  for (let i = 0; i < timed.length; i++) {
    for (let j = i + 1; j < timed.length; j++) {
      const a = timed[i]
      const b = timed[j]
      if (a.start < b.end && b.start < a.end) {
        overlapping.add(a.id)
        overlapping.add(b.id)
      }
    }
  }
  return overlapping
}
