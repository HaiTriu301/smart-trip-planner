import { useId, type ComponentProps } from 'react'

type TextAreaFieldProps = ComponentProps<'textarea'> & {
  label: string
  error?: string
}

/** Multi-line counterpart of FormField: spread register('x') into it. */
export function TextAreaField({ label, error, id, className, rows = 3, ...textareaProps }: TextAreaFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`

  return (
    <div className="space-y-1">
      <label htmlFor={inputId} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      <textarea
        id={inputId}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`block w-full rounded-lg border px-3 py-2 text-slate-900 shadow-sm outline-none focus:ring-2 ${
          error
            ? 'border-red-400 focus:ring-red-200'
            : 'border-slate-300 focus:border-sky-500 focus:ring-sky-200'
        } ${className ?? ''}`}
        {...textareaProps}
      />
      {error && (
        <p id={errorId} className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
