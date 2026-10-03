import { create } from 'zustand'

interface MapLinkState {
  /** The activity whose card is under the pointer or holds the keyboard focus; null when none */
  highlightedActivityId: number | null
  highlight: (activityId: number) => void
  /** Clears the highlight only if it still belongs to that activity: the pointer may already be on another card */
  clearHighlight: (activityId: number) => void
  /** The activity just reached from its marker: its card shows a ring for a moment; null when none */
  revealedActivityId: number | null
  reveal: (activityId: number) => void
}

/** DOM id of the card of an activity: what a marker scrolls to and focuses. */
export const activityCardId = (activityId: number) => `activity-${activityId}`

/** Long enough to find the card after the page has scrolled to it */
const REVEAL_MS = 2000

/**
 * Link between the activity cards and the map of the day (UI_GUIDE 9). The two sit in different columns, far
 * apart in the component tree; a store lets a card say "I am being looked at" and only the marker of that
 * activity redraw, without the whole day rendering again on every mouse move. The other way round, a marker
 * says "show my card" and that card marks itself.
 */
export const useMapLinkStore = create<MapLinkState>()((set, get) => ({
  highlightedActivityId: null,
  highlight: (activityId) => set({ highlightedActivityId: activityId }),
  clearHighlight: (activityId) => {
    if (get().highlightedActivityId === activityId) set({ highlightedActivityId: null })
  },
  revealedActivityId: null,
  reveal: (activityId) => {
    set({ revealedActivityId: activityId })
    // A later reveal of another activity must not be cut short by this timer
    setTimeout(() => {
      if (get().revealedActivityId === activityId) set({ revealedActivityId: null })
    }, REVEAL_MS)
  },
}))

/**
 * Brings the card of an activity into view and marks it. The card goes to the middle of the screen: aiming at
 * the top would put it under the pinned day header. The focus moves to the card, which also keeps its marker
 * highlighted, so the eye can go back and forth between the two.
 */
export function revealActivity(activityId: number) {
  const card = document.getElementById(activityCardId(activityId))
  if (!card) return
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  card.scrollIntoView({ block: 'center', behavior: reducedMotion ? 'auto' : 'smooth' })
  card.focus({ preventScroll: true })
  useMapLinkStore.getState().reveal(activityId)
}
