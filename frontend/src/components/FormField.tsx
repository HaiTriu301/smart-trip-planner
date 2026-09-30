import { useId, type ComponentProps } from 'react'

type FormFieldProps = ComponentProps<'input'> & {
  label: string
  error?: string
}

/** Labelled input for react-hook-form: spread register('x') into it (React 19 passes ref as a prop). */
export function FormField({ label, error, id, className, ...inputProps }: FormFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`

  return (
    <div className="space-y-1">
      <label htmlFor={inputId} className="block text-sm font-medium text-gray-700">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`block w-full rounded-control border px-3 py-2 text-gray-900 outline-none focus:ring-2 ${
          error
            ? 'border-danger focus:ring-danger/30'
            : 'border-gray-300 focus:border-jade focus:ring-jade/25'
        } ${className ?? ''}`}
        {...inputProps}
      />
      {error && (
        <p id={errorId} className="text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
