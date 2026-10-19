import { create } from 'zustand'

/** The two tabs of the comment panel (Task 4.6 decision 15): comments on the whole trip, comments on activities */
export type CommentTab = 'trip' | 'activities'

interface CommentPanelState {
  open: boolean
  /** The tab shown while no single activity is open */
  tab: CommentTab
  /** The activity whose comments the panel shows (opened from its card); null for a tab */
  activityId: number | null
  /** From the trip header: the "Chung" tab. From the button of an activity card: that activity */
  openPanel: (activityId?: number | null) => void
  closePanel: () => void
  /** A tab of the switch; also the way back from one activity ("×" on its chip goes to "Hoạt động") */
  showTab: (tab: CommentTab) => void
  /**
   * What was typed in the comment box and not sent yet, kept while the panel is closed (a click outside closes it,
   * maybe by mistake). Only for the trip it was typed on; the trip page forgets it when it goes away
   */
  draft: { tripId: number; content: string } | null
  saveDraft: (tripId: number, content: string) => void
  clearDraft: () => void
}

/**
 * Whether the comment panel is open, and what it shows (UI_GUIDE 8.1, Task 4.6). The panel belongs to the trip
 * header while the buttons that open it sit on every activity card, far down the tree: a store lets a card open it
 * without passing callbacks through the whole itinerary. The trip page closes it when it goes away, so a panel left
 * open never shows up on another trip. The draft of the comment box lives here too, since the box itself only
 * exists while the panel is open.
 */
export const useCommentPanelStore = create<CommentPanelState>()((set) => ({
  open: false,
  tab: 'trip',
  activityId: null,
  openPanel: (activityId = null) =>
    set(activityId === null ? { open: true, tab: 'trip', activityId: null } : { open: true, tab: 'activities', activityId }),
  closePanel: () => set({ open: false, activityId: null }),
  showTab: (tab) => set({ tab, activityId: null }),
  draft: null,
  saveDraft: (tripId, content) => set({ draft: content === '' ? null : { tripId, content } }),
  clearDraft: () => set({ draft: null }),
}))

/** The draft typed on this trip, or "" (a draft of another trip is not offered). */
export function draftFor(tripId: number): string {
  const draft = useCommentPanelStore.getState().draft
  return draft?.tripId === tripId ? draft.content : ''
}
