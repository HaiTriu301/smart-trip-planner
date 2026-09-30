import { useId, type ComponentProps } from 'react'

type SelectFieldProps = ComponentProps<'select'> & {
  label: string
  options: readonly { value: string; label: string }[]
  error?: string
}

/** Labelled <select> for react-hook-form: spread register('x') into it. */
export function SelectField({ label, options, error, id, className, ...selectProps }: SelectFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`

  return (
    <div className="space-y-1">
      <label htmlFor={inputId} className="block text-sm font-medium text-gray-700">
        {label}
      </label>
      <select
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`block w-full rounded-control border bg-white px-3 py-2 text-gray-900 outline-none focus:ring-2 ${
          error
            ? 'border-danger focus:ring-danger/30'
            : 'border-gray-300 focus:border-jade focus:ring-jade/25'
        } ${className ?? ''}`}
        {...selectProps}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && (
        <p id={errorId} className="text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
