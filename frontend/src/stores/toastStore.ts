import { create } from 'zustand'

export type ToastTone = 'success' | 'error'

/** Optional link in a toast, e.g. to the day an activity was moved to */
export interface ToastLink {
  label: string
  to: string
}

export interface Toast {
  id: number
  message: string
  tone: ToastTone
  link?: ToastLink
}

interface ToastState {
  toasts: Toast[]
  show: (message: string, tone: ToastTone, link?: ToastLink) => void
  dismiss: (id: number) => void
}

// UI_GUIDE 7.7: toasts close by themselves after 4 seconds
const AUTO_DISMISS_MS = 4000

let nextId = 1

export const useToastStore = create<ToastState>()((set, get) => ({
  toasts: [],
  show: (message, tone, link) => {
    const id = nextId++
    set({ toasts: [...get().toasts, { id, message, tone, link }] })
    setTimeout(() => get().dismiss(id), AUTO_DISMISS_MS)
  },
  dismiss: (id) => set({ toasts: get().toasts.filter((t) => t.id !== id) }),
}))

/**
 * Result of an action, worded in the completed form of the button that was pressed:
 * "Lưu thay đổi" → "Đã lưu thay đổi" (UI_GUIDE 7.7, 10). No "Hoàn tác": the API deletes immediately.
 */
export const toast = {
  success: (message: string, link?: ToastLink) => useToastStore.getState().show(message, 'success', link),
  error: (message: string) => useToastStore.getState().show(message, 'error'),
}
