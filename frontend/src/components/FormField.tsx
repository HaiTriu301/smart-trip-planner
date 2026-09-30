import { useId, type ComponentProps } from 'react'
import { FieldShell } from './FieldShell'
import { controlClass, describedBy } from './fieldStyles'

type FormFieldProps = ComponentProps<'input'> & {
  label: string
  hint?: string
  error?: string
}

/** Labelled 40px input for react-hook-form: spread register('x') into it (React 19 passes ref as a prop). */
export function FormField({ label, hint, error, required, id, className, ...inputProps }: FormFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId

  return (
    <FieldShell id={inputId} label={label} required={required} hint={hint} error={error}>
      <input
        id={inputId}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(inputId, error, hint)}
        className={`h-10 ${controlClass(Boolean(error))} ${className ?? ''}`}
        {...inputProps}
      />
    </FieldShell>
  )
}
