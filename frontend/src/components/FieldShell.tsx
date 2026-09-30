import type { ReactNode } from 'react'
import { CircleAlert } from 'lucide-react'

interface FieldShellProps {
  id: string
  label: string
  required?: boolean
  /** Caption under the field (12px), hidden while an error is shown */
  hint?: string
  error?: string
  children: ReactNode
}

/**
 * Label above, control, then hint or error below (UI_GUIDE 7.2). Required fields get a red "*" after the
 * label instead of the words "(bắt buộc)"; optional ones need no marker.
 */
export function FieldShell({ id, label, required, hint, error, children }: FieldShellProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-[13px] leading-[18px] font-medium text-gray-700">
        {label}
        {required && (
          <span aria-hidden className="ml-0.5 text-danger">
            *
          </span>
        )}
      </label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="flex items-start gap-1 text-xs leading-4 text-danger">
          <CircleAlert aria-hidden className="size-3.5 shrink-0" />
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="text-xs leading-4 text-gray-500">
            {hint}
          </p>
        )
      )}
    </div>
  )
}
