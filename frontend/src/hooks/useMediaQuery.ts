import { useSyncExternalStore } from 'react'

/**
 * True while the media query matches, e.g. useMediaQuery('(min-width: 1024px)'). For the cases CSS cannot
 * cover: deciding whether a costly component is created at all, not only whether it is visible.
 */
export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const media = window.matchMedia(query)
      media.addEventListener('change', onChange)
      return () => media.removeEventListener('change', onChange)
    },
    () => window.matchMedia(query).matches,
  )
}
