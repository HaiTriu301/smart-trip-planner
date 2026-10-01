import { useId, type ComponentProps } from 'react'
import { FieldShell } from './FieldShell'
import { controlClass, describedBy } from './fieldStyles'

type SelectFieldProps = ComponentProps<'select'> & {
  label: string
  options: readonly { value: string; label: string }[]
  hint?: string
  error?: string
}

/** Labelled 40px <select> for react-hook-form: spread register('x') into it. */
export function SelectField({ label, options, hint, error, required, id, className, ...selectProps }: SelectFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId

  return (
    <FieldShell id={inputId} label={label} required={required} hint={hint} error={error}>
      <select
        id={inputId}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(inputId, error, hint)}
        className={`h-10 w-full px-3 ${controlClass(Boolean(error))} ${className ?? ''}`}
        {...selectProps}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </FieldShell>
  )
}
