import { Link } from 'react-router-dom'
import { CircleAlert, CircleCheck, X } from 'lucide-react'
import { useToastStore } from '../stores/toastStore'

/**
 * Toast stack: bottom right on desktop, top on phones, 360px wide (UI_GUIDE 7.7). The container is a polite
 * live region so screen readers read each new toast; errors use role="alert" (UI_GUIDE 12).
 */
export function Toaster() {
  const toasts = useToastStore((s) => s.toasts)
  const dismiss = useToastStore((s) => s.dismiss)

  return (
    <div
      role="status"
      aria-live="polite"
      className="pointer-events-none fixed inset-x-4 top-4 z-50 flex flex-col gap-2 sm:inset-x-auto sm:top-auto sm:right-6 sm:bottom-6 sm:w-[360px]"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          role={t.tone === 'error' ? 'alert' : undefined}
          className="pointer-events-auto flex items-start gap-3 rounded-card border border-tide bg-white p-3 text-sm text-ink shadow-md"
        >
          {t.tone === 'success' ? (
            <CircleCheck aria-hidden className="mt-0.5 size-4 shrink-0 text-success" />
          ) : (
            <CircleAlert aria-hidden className="mt-0.5 size-4 shrink-0 text-danger" />
          )}
          <div className="min-w-0 flex-1">
            <p>{t.message}</p>
            {t.link && (
              <Link to={t.link.to} onClick={() => dismiss(t.id)} className="font-medium text-jade hover:underline">
                {t.link.label}
              </Link>
            )}
          </div>
          <button
            type="button"
            aria-label="Đóng thông báo"
            onClick={() => dismiss(t.id)}
            className="-m-1 rounded-control p-1 text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
          >
            <X aria-hidden className="size-4" />
          </button>
        </div>
      ))}
    </div>
  )
}
