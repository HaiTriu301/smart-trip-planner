import type { ReactNode } from 'react'
import { badgeToneClass, type BadgeTone } from './badgeStyles'

interface BadgeProps {
  tone: BadgeTone
  /** solid: white background, for badges placed over a cover photo */
  surface?: 'tint' | 'solid'
  children: ReactNode
}

/** 20px label, 6px radius, 12px/500 (UI_GUIDE 7.5): trip status, member role, Premium. */
export function Badge({ tone, surface = 'tint', children }: BadgeProps) {
  return (
    <span
      className={`inline-flex h-5 shrink-0 items-center rounded-control px-2 text-xs leading-none font-medium whitespace-nowrap ${badgeToneClass(tone, surface)}`}
    >
      {children}
    </span>
  )
}
