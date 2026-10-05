import { useSyncExternalStore } from 'react'
import { DEFAULT_TIME_ZONE, todayIn } from '../lib/today'
import { useAuthStore } from '../stores/authStore'

/** How often an open page checks whether the day has changed */
const CHECK_EVERY_MS = 60_000

function subscribe(onChange: () => void) {
  const timer = window.setInterval(onChange, CHECK_EVERY_MS)
  // A tab left in the background has its timers slowed down: look again as soon as it is back
  window.addEventListener('focus', onChange)
  document.addEventListener('visibilitychange', onChange)
  return () => {
    window.clearInterval(timer)
    window.removeEventListener('focus', onChange)
    document.removeEventListener('visibilitychange', onChange)
  }
}

/**
 * Today as "YYYY-MM-DD" in the time zone of the signed-in account (design.md rule 14.22). The one place
 * components get "today" from: none of them calls `new Date()` itself. A page left open past midnight moves on
 * to the new day by itself, within a minute.
 */
export function useToday(): string {
  const timeZone = useAuthStore((state) => state.user?.timezone) ?? DEFAULT_TIME_ZONE
  // The value is a string, the same one all day long: components redraw only when the day changes
  return useSyncExternalStore(subscribe, () => todayIn(timeZone))
}
