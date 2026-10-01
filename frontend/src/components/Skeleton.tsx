/** Placeholder block while content loads: gray-100 with a sweeping highlight (UI_GUIDE 7.8). Size it with className. */
export function Skeleton({ className }: { className: string }) {
  return (
    <div
      aria-hidden
      className={`animate-shimmer rounded-control bg-[linear-gradient(90deg,var(--color-gray-100)_0%,var(--color-gray-50)_50%,var(--color-gray-100)_100%)] bg-[length:200%_100%] ${className}`}
    />
  )
}
