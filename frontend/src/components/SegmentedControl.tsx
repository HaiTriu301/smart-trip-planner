import { useId } from 'react'
import { CircleAlert, type LucideIcon } from 'lucide-react'
import type { UseFormRegisterReturn } from 'react-hook-form'

export interface SegmentOption {
  value: string
  label: string
  icon?: LucideIcon
}

interface SegmentedControlProps {
  label: string
  options: readonly SegmentOption[]
  /** register('x') of the form: every segment shares it */
  registration: UseFormRegisterReturn
  error?: string
}

/**
 * Two to four choices side by side on a gray track, the chosen one raised in white (UI_GUIDE 7.15, Task 4.9
 * decision 2): one click picks, nothing opens. Like ActivityTypeField, the segments are real radio inputs hidden
 * behind their labels: Tab reaches the group, the arrow keys move inside it, a screen reader reads a group of
 * N choices, and react-hook-form reads the value through register(). It fills the width of its box: a grid
 * column sized "auto" makes it as wide as its labels.
 */
export function SegmentedControl({ label, options, registration, error }: SegmentedControlProps) {
  const id = useId()

  return (
    <fieldset aria-describedby={error ? `${id}-error` : undefined} className="space-y-1.5">
      <legend className="mb-1.5 block text-[13px] leading-[18px] font-medium text-gray-700">{label}</legend>
      {/* 40px like a text field (44px on touch screens), 4px of track around the segments */}
      <div className="flex h-10 w-full gap-1 rounded-control bg-gray-200 p-1 pointer-coarse:h-11">
        {options.map((option) => (
          <div key={option.value} className="relative flex min-w-0 flex-1">
            <input type="radio" id={`${id}-${option.value}`} value={option.value} className="peer sr-only" {...registration} />
            {/* Chosen and not chosen set colours through two opposite variants, never one class over another
                (BUG-UI-001) */}
            <label
              htmlFor={`${id}-${option.value}`}
              className="flex min-w-0 flex-1 cursor-pointer items-center justify-center gap-1.5 rounded-[4px] px-3 text-sm whitespace-nowrap transition-[color,background-color,box-shadow] duration-150 peer-checked:bg-white peer-checked:font-medium peer-checked:text-ink peer-checked:shadow-sm peer-focus-visible:outline-2 peer-focus-visible:outline-offset-1 peer-focus-visible:outline-jade peer-[:not(:checked)]:text-gray-600 peer-[:not(:checked)]:hover:text-ink motion-reduce:transition-none"
            >
              {option.icon && <option.icon aria-hidden className="size-4 shrink-0" />}
              {option.label}
            </label>
          </div>
        ))}
      </div>
      {error && (
        <p id={`${id}-error`} className="flex items-start gap-1 text-xs leading-4 text-danger">
          <CircleAlert aria-hidden className="size-3.5 shrink-0" />
          {error}
        </p>
      )}
    </fieldset>
  )
}
