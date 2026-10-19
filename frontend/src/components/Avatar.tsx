import { initials } from '../lib/initials'

interface AvatarProps {
  /** null for an invited email without an account */
  name: string | null
  /** Only for the initials of an invited email without a name: a comment author has a name and no email */
  email?: string
  url: string | null
  /** An invitation not accepted yet: amber instead of jade, as in the Stitch mockup (Task 4.5) */
  pending?: boolean
}

/**
 * 32px round picture of a person, or their initials on a tinted circle when there is no picture (accounts have
 * none yet). Decorative: the name or email is always written next to it.
 */
export function Avatar({ name, email, url, pending = false }: AvatarProps) {
  if (url) return <img src={url} alt="" className="size-8 shrink-0 rounded-full object-cover" />
  return (
    <span
      aria-hidden
      className={`flex size-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
        pending ? 'bg-warning/15 text-gray-800' : 'bg-jade-light text-jade-dark'
      }`}
    >
      {initials(name, email ?? '')}
    </span>
  )
}
