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
      <label htmlFor={inputId} className="block text-sm font-medium text-gray-700">
        {label}
      </label>
      <textarea
        id={inputId}
        rows={rows}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`block w-full rounded-control border px-3 py-2 text-gray-900 outline-none focus:ring-2 ${
          error
            ? 'border-danger focus:ring-danger/30'
            : 'border-gray-300 focus:border-jade focus:ring-jade/25'
        } ${className ?? ''}`}
        {...textareaProps}
      />
      {error && (
        <p id={errorId} className="text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
