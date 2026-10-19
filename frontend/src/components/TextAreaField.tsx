import { useCallback, useId, useState, type ChangeEvent, type ComponentProps } from 'react'
import { charCounter } from '../lib/charCounter'
import { FieldShell } from './FieldShell'
import { controlClass, describedBy } from './fieldStyles'

type TextAreaFieldProps = Omit<ComponentProps<'textarea'>, 'rows'> & {
  label: string
  /** Label for screen readers only (FieldShell hideLabel) */
  hideLabel?: boolean
  hint?: string
  error?: string
}

// Browsers that size a textarea to its content by themselves; the others get their height set from scrollHeight
const growsByItself = typeof CSS !== 'undefined' && CSS.supports('field-sizing', 'content')

/** Fallback for browsers without field-sizing: as tall as the text, the CSS min / max height still apply. */
function fitHeight(node: HTMLTextAreaElement) {
  node.style.height = 'auto'
  // scrollHeight leaves out the 1px border on each side, the box is border-box
  node.style.height = `${node.scrollHeight + 2}px`
}

/**
 * Multi-line counterpart of FormField: spread register('x') into it (UI_GUIDE 7.2). It grows with its content from
 * 3 to 8 lines, then scrolls (Task 4.9 decision 5). With a maxLength, a counter "230 / 255" shows at the right of
 * the hint once 80% of the limit is used.
 */
export function TextAreaField({
  label,
  hideLabel,
  hint,
  error,
  required,
  id,
  className,
  maxLength,
  onChange,
  ref,
  ...textareaProps
}: TextAreaFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const [length, setLength] = useState(0)

  // react-hook-form writes the default value straight into the element inside its own ref callback, without an
  // input event: read it right after, so the counter and the height start from the stored note. Later changes
  // come through onChange. (No form with a note field calls reset(); one that does must remount the field.)
  const setRefs = useCallback(
    (node: HTMLTextAreaElement | null) => {
      if (typeof ref === 'function') ref(node)
      else if (ref) ref.current = node
      if (!node) return
      setLength(node.value.length)
      if (!growsByItself) fitHeight(node)
    },
    [ref],
  )

  function handleChange(event: ChangeEvent<HTMLTextAreaElement>) {
    setLength(event.target.value.length)
    if (!growsByItself) fitHeight(event.target)
    onChange?.(event)
  }

  const counter = charCounter(length, maxLength)

  return (
    <FieldShell
      id={inputId}
      label={label}
      hideLabel={hideLabel}
      required={required}
      hint={hint}
      error={error}
      aside={
        counter && (
          <span className={`tabular text-xs leading-4 ${counter.full ? 'font-medium text-gray-800' : 'text-gray-500'}`}>
            {counter.text}
          </span>
        )
      }
    >
      {/* 24px lines + 8px padding top and bottom + 1px border: 90px = 3 lines, 210px = 8 lines */}
      <textarea
        ref={setRefs}
        id={inputId}
        required={required}
        maxLength={maxLength}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(inputId, error, hint)}
        onChange={handleChange}
        className={`field-sizing-content max-h-[210px] min-h-[90px] w-full resize-none overflow-y-auto px-3 py-2 leading-6 ${controlClass(Boolean(error))} ${className ?? ''}`}
        {...textareaProps}
      />
    </FieldShell>
  )
}
