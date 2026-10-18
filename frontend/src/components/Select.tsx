import { useId, type KeyboardEvent, type Ref } from 'react'
import * as SelectPrimitive from '@radix-ui/react-select'
import { Check, ChevronDown } from 'lucide-react'
import { FieldShell } from './FieldShell'
import { controlLookClass, describedBy } from './fieldStyles'
import { useOverlayContainer } from './overlayContainer'

export interface SelectOption {
  value: string
  /** Shown in the list and, once chosen, in the field */
  label: string
  /** Second line in the list only, e.g. "Đồng Việt Nam" under "VND" */
  description?: string
}

interface SelectProps {
  options: readonly SelectOption[]
  value: string
  onValueChange: (value: string) => void
  onBlur?: () => void
  /** For react-hook-form: setFocus() and the first-error focus land on the button */
  ref?: Ref<HTMLButtonElement>
  id?: string
  name?: string
  placeholder?: string
  disabled?: boolean
  required?: boolean
  invalid?: boolean
  /** Needed when no visible label points at the field */
  'aria-label'?: string
  'aria-describedby'?: string
}

// Esc closes the list (Radix listens on the document first); stopping the key here keeps the browser from also
// treating it as a "close the dialog" request when the field sits in a Modal (Task 4.9 trap 2)
function keepEscapeInList(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  event.preventDefault()
  event.stopPropagation()
}

/**
 * A select drawn by us on Radix Select (UI_GUIDE 7.2, Task 4.9 decision 1; the shadcn/ui Select was the template,
 * rewritten with the project's tokens). The closed field looks like a text field, with a chevron; the list floats
 * below it with a check on the chosen row, a light teal row under the pointer or the arrow keys, an optional
 * second line per row, and typing a letter jumps to the matching row. Inside a Modal the list is drawn into the
 * <dialog>, which would cover it otherwise.
 */
export function Select({
  options,
  value,
  onValueChange,
  onBlur,
  ref,
  id,
  name,
  placeholder,
  disabled,
  required,
  invalid = false,
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedBy,
}: SelectProps) {
  const container = useOverlayContainer()

  return (
    <SelectPrimitive.Root
      value={value}
      onValueChange={onValueChange}
      name={name}
      disabled={disabled}
      required={required}
    >
      <SelectPrimitive.Trigger
        ref={ref}
        id={id}
        onBlur={onBlur}
        aria-label={ariaLabel}
        aria-invalid={invalid || undefined}
        aria-describedby={ariaDescribedBy}
        className={`group flex h-10 w-full cursor-pointer items-center justify-between gap-2 px-3 text-left data-[placeholder]:text-gray-400 pointer-coarse:h-11 ${controlLookClass(invalid)}`}
      >
        <span className="min-w-0 truncate">
          <SelectPrimitive.Value placeholder={placeholder} />
        </span>
        <SelectPrimitive.Icon asChild>
          <ChevronDown
            aria-hidden
            className="size-4 shrink-0 text-gray-500 transition-transform duration-150 group-data-[state=open]:rotate-180 motion-reduce:transition-none"
          />
        </SelectPrimitive.Icon>
      </SelectPrimitive.Trigger>
      <SelectPrimitive.Portal container={container ?? undefined}>
        <SelectPrimitive.Content
          position="popper"
          sideOffset={4}
          onKeyDown={keepEscapeInList}
          // At least as wide as the field and 11rem, so "Đồng Việt Nam" fits under a narrow currency field; at most
          // as wide as the field (20rem for a narrow one), so a long second line wraps instead of widening the
          // list past the dialog; never taller than the room left on screen
          className="z-50 max-h-(--radix-select-content-available-height) max-w-[max(var(--radix-select-trigger-width),20rem)] min-w-[max(var(--radix-select-trigger-width),11rem)] origin-(--radix-select-content-transform-origin) overflow-hidden rounded-control border border-tide bg-white text-ink shadow-md data-[state=closed]:animate-pop-out data-[state=open]:animate-pop-in motion-reduce:animate-none"
        >
          <SelectPrimitive.Viewport className="max-h-72 p-1">
            {options.map((option) => (
              <SelectPrimitive.Item
                key={option.value}
                value={option.value}
                className="relative flex cursor-pointer flex-col rounded-[4px] py-2 pr-9 pl-2.5 outline-none select-none data-[highlighted]:bg-jade-light pointer-coarse:py-3"
              >
                <SelectPrimitive.ItemText>
                  <span className="text-[15px] leading-5">{option.label}</span>
                </SelectPrimitive.ItemText>
                {option.description && <span className="text-[13px] leading-[18px] text-gray-600">{option.description}</span>}
                <span className="absolute top-1/2 right-2.5 flex size-4 -translate-y-1/2 items-center justify-center">
                  <SelectPrimitive.ItemIndicator>
                    <Check aria-hidden className="size-4 text-jade" />
                  </SelectPrimitive.ItemIndicator>
                </span>
              </SelectPrimitive.Item>
            ))}
          </SelectPrimitive.Viewport>
        </SelectPrimitive.Content>
      </SelectPrimitive.Portal>
    </SelectPrimitive.Root>
  )
}

type SelectFieldProps = Omit<SelectProps, 'invalid' | 'aria-describedby'> & {
  label: string
  hint?: string
  error?: string
}

/**
 * Labelled Select for a form (label, "*", hint, error: FieldShell). Not for register(): wrap it in a react-hook-form
 * Controller and pass field.value / onChange / onBlur / ref.
 */
export function SelectField({ label, hint, error, required, id, ...selectProps }: SelectFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId

  return (
    <FieldShell id={inputId} label={label} required={required} hint={hint} error={error}>
      <Select
        id={inputId}
        required={required}
        invalid={Boolean(error)}
        aria-describedby={describedBy(inputId, error, hint)}
        {...selectProps}
      />
    </FieldShell>
  )
}
