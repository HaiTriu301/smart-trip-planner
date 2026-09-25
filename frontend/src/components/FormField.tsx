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
      <label htmlFor={inputId} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`block w-full rounded-lg border px-3 py-2 text-slate-900 shadow-sm outline-none focus:ring-2 ${
          error
            ? 'border-red-400 focus:ring-red-200'
            : 'border-slate-300 focus:border-sky-500 focus:ring-sky-200'
        } ${className ?? ''}`}
        {...inputProps}
      />
      {error && (
        <p id={errorId} className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
