import { create } from 'zustand'
import { useAuthStore } from './authStore'

// Trips the user answered "Để sau" for when asked whether they are completed (design.md rule 14.22): not asked
// again until the next sign-in. The list is kept in localStorage, not in memory: the access token lives in
// memory only, so a reload restores the session silently and is not a new sign-in. Only trip ids are stored,
// nothing that identifies or authenticates anyone (CLAUDE.md rule 36 is about tokens).

const STORAGE_KEY = 'trip-planner.complete-later'

type KeyValueStorage = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>

/** localStorage, or nothing where it is not available (private mode, blocked site data, tests) */
function browserStorage(): KeyValueStorage | undefined {
  try {
    return typeof window === 'undefined' ? undefined : window.localStorage
  } catch {
    return undefined
  }
}

/** The stored ids; an empty list for anything missing, unreadable or not a list of whole numbers. */
export function readPostponed(storage: KeyValueStorage | undefined): number[] {
  try {
    const parsed: unknown = JSON.parse(storage?.getItem(STORAGE_KEY) ?? '[]')
    return Array.isArray(parsed) ? parsed.filter((id): id is number => Number.isInteger(id)) : []
  } catch {
    return []
  }
}

/** Stores the ids; an empty list removes the entry. A storage that refuses the write is ignored. */
export function writePostponed(storage: KeyValueStorage | undefined, tripIds: readonly number[]): void {
  try {
    if (tripIds.length === 0) storage?.removeItem(STORAGE_KEY)
    else storage?.setItem(STORAGE_KEY, JSON.stringify(tripIds))
  } catch {
    // Not being able to remember costs one extra question after a reload; the page must keep working
  }
}

interface CompletePromptState {
  postponedTripIds: number[]
  /** "Để sau": do not ask about this trip again during this sign-in */
  postpone: (tripId: number) => void
  clear: () => void
}

export const useCompletePromptStore = create<CompletePromptState>()((set, get) => ({
  postponedTripIds: readPostponed(browserStorage()),
  postpone: (tripId) => {
    if (get().postponedTripIds.includes(tripId)) return
    const postponedTripIds = [...get().postponedTripIds, tripId]
    writePostponed(browserStorage(), postponedTripIds)
    set({ postponedTripIds })
  },
  clear: () => {
    writePostponed(browserStorage(), [])
    set({ postponedTripIds: [] })
  },
}))

// The list ends with the session, however it ends: the logout button, a rejected refresh, or a visit that
// finds the old session expired (unknown → anonymous). A reload with a live session (unknown → authenticated)
// keeps it. This module is loaded with the app because the pages are imported statically; if the trip page is
// ever loaded on demand, import this file from main.tsx so the subscription still exists at sign-out.
useAuthStore.subscribe((state, previous) => {
  if (state.status === 'anonymous' && previous.status !== 'anonymous') useCompletePromptStore.getState().clear()
})
