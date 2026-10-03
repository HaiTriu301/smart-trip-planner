import { create } from 'zustand'

interface MapLinkState {
  /** The activity whose card is under the pointer or holds the keyboard focus; null when none */
  highlightedActivityId: number | null
  highlight: (activityId: number) => void
  /** Clears the highlight only if it still belongs to that activity: the pointer may already be on another card */
  clearHighlight: (activityId: number) => void
}

/**
 * Link between the activity cards and the map of the day (UI_GUIDE 9). The two sit in different columns, far
 * apart in the component tree; a store lets a card say "I am being looked at" and only the marker of that
 * activity redraw, without the whole day rendering again on every mouse move.
 */
export const useMapLinkStore = create<MapLinkState>()((set, get) => ({
  highlightedActivityId: null,
  highlight: (activityId) => set({ highlightedActivityId: activityId }),
  clearHighlight: (activityId) => {
    if (get().highlightedActivityId === activityId) set({ highlightedActivityId: null })
  },
}))
