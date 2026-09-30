import { useId, useState, type ComponentProps, type ReactNode } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { FieldShell } from './FieldShell'
import { controlClass, describedBy } from './fieldStyles'

type PasswordFieldProps = Omit<ComponentProps<'input'>, 'type'> & {
  label: string
  hint?: string
  error?: string
  labelAction?: ReactNode
}

/** Password input with an eye button to show or hide what was typed (Stitch mockup). */
export function PasswordField({ label, hint, error, labelAction, required, id, className, ...inputProps }: PasswordFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const [visible, setVisible] = useState(false)

  return (
    <FieldShell id={inputId} label={label} required={required} hint={hint} error={error} labelAction={labelAction}>
      <div className="relative">
        <input
          id={inputId}
          type={visible ? 'text' : 'password'}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(inputId, error, hint)}
          className={`h-10 w-full pr-11 pl-3 ${controlClass(Boolean(error))} ${className ?? ''}`}
          {...inputProps}
        />
        <button
          type="button"
          aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          aria-pressed={visible}
          aria-controls={inputId}
          onClick={() => setVisible(!visible)}
          className="absolute inset-y-0 right-0 flex w-10 items-center justify-center rounded-r-control text-gray-500 hover:text-gray-900 focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none"
        >
          {visible ? <EyeOff aria-hidden className="size-4" /> : <Eye aria-hidden className="size-4" />}
        </button>
      </div>
    </FieldShell>
  )
}
