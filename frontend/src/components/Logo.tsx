import { MapPin } from 'lucide-react'

interface LogoProps {
  /** Larger mark with a line under the wordmark, for the sign-in pages */
  tagline?: string
}

/** Pin mark on a jade tile + ink wordmark, as in the Stitch mockups. Every place it stands on is light. */
export function Logo({ tagline }: LogoProps) {
  if (tagline) {
    return (
      <span className="inline-flex items-center gap-3">
        <span className="flex size-11 items-center justify-center rounded-control bg-jade text-white" aria-hidden>
          <MapPin className="size-6" strokeWidth={2.25} />
        </span>
        <span className="text-left">
          <span className="block text-2xl leading-8 font-bold tracking-[-0.01em] text-ink">Smart Trip Planner</span>
          <span className="block text-sm text-gray-600">{tagline}</span>
        </span>
      </span>
    )
  }

  return (
    <span className="inline-flex items-center gap-2">
      <span className="flex size-8 items-center justify-center rounded-control bg-jade text-white" aria-hidden>
        <MapPin className="size-5" strokeWidth={2.25} />
      </span>
      <span className="text-lg leading-none font-bold tracking-[-0.01em] text-ink">Smart Trip Planner</span>
    </span>
  )
}
