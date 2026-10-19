import { create } from 'zustand'

interface CommentPanelState {
  open: boolean
  /** The activity whose comments the panel shows (opened from its card); null for every comment of the trip */
  activityId: number | null
  /** From the trip header (no activity) or from the button of an activity card */
  openPanel: (activityId?: number | null) => void
  closePanel: () => void
  /** "×" on the activity chip at the top of the panel: back to every comment of the trip */
  showAllComments: () => void
  /**
   * What was typed in the comment box and not sent yet, kept while the panel is closed (a click outside closes it,
   * maybe by mistake). Only for the trip it was typed on; the trip page forgets it when it goes away
   */
  draft: { tripId: number; content: string } | null
  saveDraft: (tripId: number, content: string) => void
  clearDraft: () => void
}

/**
 * Whether the comment panel is open, and on which activity (UI_GUIDE 8.1, Task 4.6). The panel belongs to the trip
 * header while the buttons that open it sit on every activity card, far down the tree: a store lets a card open it
 * without passing callbacks through the whole itinerary. The trip page closes it when it goes away, so a panel left
 * open never shows up on another trip. The draft of the comment box lives here too, since the box itself only
 * exists while the panel is open.
 */
export const useCommentPanelStore = create<CommentPanelState>()((set) => ({
  open: false,
  activityId: null,
  openPanel: (activityId = null) => set({ open: true, activityId }),
  closePanel: () => set({ open: false, activityId: null }),
  showAllComments: () => set({ activityId: null }),
  draft: null,
  saveDraft: (tripId, content) => set({ draft: content === '' ? null : { tripId, content } }),
  clearDraft: () => set({ draft: null }),
}))

/** The draft typed on this trip, or "" (a draft of another trip is not offered). */
export function draftFor(tripId: number): string {
  const draft = useCommentPanelStore.getState().draft
  return draft?.tripId === tripId ? draft.content : ''
}
