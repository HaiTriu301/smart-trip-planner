import { useId, type ComponentProps } from 'react'
import { FieldShell } from './FieldShell'
import { controlClass, describedBy } from './fieldStyles'

type TextAreaFieldProps = ComponentProps<'textarea'> & {
  label: string
  hint?: string
  error?: string
}

/** Multi-line counterpart of FormField: spread register('x') into it. */
export function TextAreaField({ label, hint, error, required, id, className, rows = 3, ...textareaProps }: TextAreaFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId

  return (
    <FieldShell id={inputId} label={label} required={required} hint={hint} error={error}>
      <textarea
        id={inputId}
        rows={rows}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(inputId, error, hint)}
        className={`w-full px-3 py-2 ${controlClass(Boolean(error))} ${className ?? ''}`}
        {...textareaProps}
      />
    </FieldShell>
  )
}
