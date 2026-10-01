import { useSyncExternalStore } from 'react'

function subscribe(onChange: () => void) {
  window.addEventListener('scroll', onChange, { passive: true })
  window.addEventListener('resize', onChange)
  return () => {
    window.removeEventListener('scroll', onChange)
    window.removeEventListener('resize', onChange)
  }
}

// One screen down counts as "deep": the top of the page is no longer in sight
const isPastOneScreen = () => window.scrollY > window.innerHeight

/**
 * True once the page has been scrolled further than one screen height. The snapshot is a boolean, so a
 * component using it renders again only when the answer flips, not on every scroll event.
 */
export function useScrolledPast(): boolean {
  return useSyncExternalStore(subscribe, isPastOneScreen)
}
