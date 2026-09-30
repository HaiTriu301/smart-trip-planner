import { MapPin } from 'lucide-react'

interface LogoProps {
  /** light: ink text on the paper background · dark: white text on the ink top bar */
  tone: 'light' | 'dark'
}

/** Pin mark on a jade tile + wordmark, as in the Stitch mockups. */
export function Logo({ tone }: LogoProps) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className="flex size-8 items-center justify-center rounded-control bg-jade text-white" aria-hidden>
        <MapPin className="size-5" strokeWidth={2.25} />
      </span>
      <span className={`text-lg leading-none font-bold tracking-[-0.01em] ${tone === 'dark' ? 'text-white' : 'text-ink'}`}>
        Smart Trip Planner
      </span>
    </span>
  )
}
