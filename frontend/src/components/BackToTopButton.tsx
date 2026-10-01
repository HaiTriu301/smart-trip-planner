import { ArrowUp } from 'lucide-react'
import { useScrolledPast } from '../hooks/useScrolledPast'
import { Button } from './Button'

// Position, size and display per screen width, each in one place so no class has to override another.
// Both stay above the 48px footer once the page is scrolled to the very end (56px and 96px from the bottom).
// narrow: 48px touch target; phones bottom right (toasts show at the top there), tablets bottom left (toasts take
// the bottom right). wide: a small 40px button in the bottom right corner, above the toast stack (24px from the
// bottom, about 44-68px tall for one toast), so a toast does not cover it.
const FLOATING = {
  narrow: 'inline-flex size-12 right-4 bottom-14 sm:right-auto sm:left-6 lg:hidden',
  wide: 'hidden size-10 right-6 bottom-24 lg:inline-flex',
} as const

interface BackToTopButtonProps {
  /**
   * inline: a ghost button in the flow (in the pinned day header, wide screens).
   * floating: a round button pinned to a bottom corner of the screen.
   */
  placement: 'inline' | 'floating'
  /** Floating only: which screens show it, narrow (below 1024px) or wide */
  screens?: 'narrow' | 'wide'
  label: string
  /**
   * Id of the element to bring back to the top of the screen and focus ("Đầu ngày": the day section, whose
   * scroll-margin keeps it clear of the sticky bars). Without it: the top of the page and its data-page-top
   * element (the trip title).
   */
  targetId?: string
}

/**
 * "Lên đầu trang" / "Đầu ngày" (UI_GUIDE 8.1 "Ngày dài"): shown only once the page is scrolled more than one
 * screen down. The focus moves to where the page lands, so keyboard users go on from there instead of from a
 * button that has just disappeared.
 */
export function BackToTopButton({ placement, screens = 'narrow', label, targetId }: BackToTopButtonProps) {
  const scrolledPast = useScrolledPast()
  if (!scrolledPast) return null

  function scrollBack() {
    const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
    const target = targetId ? document.getElementById(targetId) : null
    if (target) {
      target.scrollIntoView({ behavior, block: 'start' })
      target.focus({ preventScroll: true })
      return
    }
    window.scrollTo({ top: 0, behavior })
    document.querySelector<HTMLElement>('[data-page-top]')?.focus({ preventScroll: true })
  }

  if (placement === 'inline') {
    return (
      <Button variant="ghost" size="sm" fullWidth={false} onClick={scrollBack}>
        <ArrowUp aria-hidden className="size-4" />
        {label}
      </Button>
    )
  }

  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={scrollBack}
      className={`fixed z-30 items-center justify-center rounded-full bg-ink text-white shadow-lg transition-colors hover:bg-ink/90 focus-visible:ring-[3px] focus-visible:ring-jade/40 focus-visible:outline-none ${FLOATING[screens]}`}
    >
      <ArrowUp aria-hidden className={screens === 'wide' ? 'size-4' : 'size-5'} />
    </button>
  )
}
