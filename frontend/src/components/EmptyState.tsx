import type { ReactNode } from 'react'

interface EmptyStateProps {
  /** One sentence saying what to do next */
  message: string
  /** The button that does it (UI_GUIDE 1.2 rule 5: an empty screen is an invitation) */
  action?: ReactNode
}

/** Line illustration + sentence + action, no card around it (UI_GUIDE 7.9). */
export function EmptyState({ message, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center gap-4 px-4 py-12 text-center">
      <FoldedMapIllustration />
      <p className="max-w-[46ch] text-gray-600">{message}</p>
      {action}
    </div>
  )
}

/** Folded map with a route and two stops: two colours, single stroke weight. */
function FoldedMapIllustration() {
  return (
    <svg aria-hidden width="160" height="112" viewBox="0 0 160 112" fill="none" strokeWidth="2" strokeLinejoin="round">
      <path d="M12 20 52 8l56 12 40-12v84l-40 12-56-12-40 12V20Z" stroke="var(--color-gray-300)" />
      <path d="M52 8v84M108 20v84" stroke="var(--color-gray-300)" />
      <path
        d="M30 78c10-4 14-18 26-20s18 10 30 6 12-24 24-28 18 2 24-4"
        stroke="var(--color-jade)"
        strokeDasharray="5 5"
        strokeLinecap="round"
      />
      <circle cx="30" cy="78" r="5" fill="white" stroke="var(--color-jade)" strokeWidth="3" />
      <circle cx="134" cy="30" r="5" fill="white" stroke="var(--color-jade)" strokeWidth="3" />
    </svg>
  )
}
