import { LoaderCircle } from 'lucide-react'

/** Only while the session is restored at startup; lists and itineraries use skeletons instead (UI_GUIDE 7.8). */
export function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center" role="status">
      <LoaderCircle aria-hidden className="size-8 animate-spin text-jade" />
      <span className="sr-only">Đang tải...</span>
    </div>
  )
}
