import type { ReactNode } from 'react'
import { CircleAlert } from 'lucide-react'

interface FieldShellProps {
  id: string
  label: string
  required?: boolean
  /** Caption under the field (12px), hidden while an error is shown */
  hint?: string
  error?: string
  /** Link or button at the right end of the label row, e.g. "Quên mật khẩu?" */
  labelAction?: ReactNode
  /** Small text at the right end of the hint / error row, e.g. the character counter of a note field */
  aside?: ReactNode
  children: ReactNode
}

/**
 * Label above, control, then hint or error below (UI_GUIDE 7.2). Required fields get a red "*" after the
 * label instead of the words "(bắt buộc)"; optional ones need no marker.
 */
export function FieldShell({ id, label, required, hint, error, labelAction, aside, children }: FieldShellProps) {
  const note = error ? (
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
  )

  return (
    <div className="space-y-1.5">
      <div className="flex items-baseline justify-between gap-3">
        <label htmlFor={id} className="block text-[13px] leading-[18px] font-medium text-gray-700">
          {label}
          {required && (
            <span aria-hidden className="ml-0.5 text-danger">
              *
            </span>
          )}
        </label>
        {labelAction}
      </div>
      {children}
      {aside ? (
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">{note}</div>
          <div className="shrink-0">{aside}</div>
        </div>
      ) : (
        note
      )}
    </div>
  )
}
