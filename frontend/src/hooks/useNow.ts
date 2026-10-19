import { useSyncExternalStore } from 'react'

const MINUTE = 60_000
/** How often the clock is looked at; components redraw only when the minute has changed */
const CHECK_EVERY_MS = 15_000

// One timer for the whole page, however many "5 phút trước" are on screen
const listeners = new Set<() => void>()
let timer: number | undefined

function notify() {
  listeners.forEach((listener) => listener())
}

function subscribe(onChange: () => void) {
  listeners.add(onChange)
  if (listeners.size === 1) {
    timer = window.setInterval(notify, CHECK_EVERY_MS)
    // A tab left in the background has its timers slowed down: look again as soon as it is back
    window.addEventListener('focus', notify)
    document.addEventListener('visibilitychange', notify)
  }
  return () => {
    listeners.delete(onChange)
    if (listeners.size === 0) {
      window.clearInterval(timer)
      window.removeEventListener('focus', notify)
      document.removeEventListener('visibilitychange', notify)
    }
  }
}

/** The current minute as a number: the same value all minute long, so nothing redraws in between. */
const currentMinute = () => Math.floor(Date.now() / MINUTE)

/**
 * "Now", to the minute, for texts such as "5 phút trước" that must move on while the page stays open (Task 4.6).
 * useToday changes once a day and cannot do this. Only the small component that writes the time should call it,
 * so a list does not redraw every minute. The value is the start of the current minute: at most a minute behind,
 * which "Vừa xong" covers.
 */
export function useNow(): Date {
  const minute = useSyncExternalStore(subscribe, currentMinute)
  return new Date(minute * MINUTE)
}
