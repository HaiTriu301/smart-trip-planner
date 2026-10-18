import { useId } from 'react'
import { CircleAlert } from 'lucide-react'
import type { UseFormRegisterReturn } from 'react-hook-form'

export interface ChipOption {
  value: string
  label: string
}

interface ChoiceChipsProps {
  label: string
  options: readonly ChipOption[]
  /** register('x') of the form: every chip shares it */
  registration: UseFormRegisterReturn
  /**
   * A click (or tap) on a chip, not a move with the arrow keys: lets "Chọn ngày…" open its calendar only when
   * pressed, so walking through the chips with the keyboard does not pull the focus away
   */
  onChipClick?: (value: string) => void
  hint?: string
  error?: string
}

/**
 * One choice among a few short ones, as pills that wrap to a new line on a narrow screen (UI_GUIDE 7.19,
 * Task 4.9): the expiry of a share link. Like SegmentedControl (7.15) they are real radio inputs behind their labels:
 * Tab reaches the group, the arrow keys move inside it, react-hook-form reads the value through register().
 */
export function ChoiceChips({ label, options, registration, onChipClick, hint, error }: ChoiceChipsProps) {
  const id = useId()
  const noteId = error ? `${id}-error` : hint ? `${id}-hint` : undefined

  return (
    <fieldset aria-describedby={noteId} className="space-y-1.5">
      <legend className="mb-1.5 block text-[13px] leading-[18px] font-medium text-gray-700">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => (
          <div key={option.value} className="relative">
            <input type="radio" id={`${id}-${option.value}`} value={option.value} className="peer sr-only" {...registration} />
            {/* Chosen and not chosen through two opposite variants, never one class over another (BUG-UI-001) */}
            <label
              htmlFor={`${id}-${option.value}`}
              onClick={() => onChipClick?.(option.value)}
              className="flex h-8 cursor-pointer items-center rounded-full border px-3 text-sm whitespace-nowrap transition-colors duration-150 peer-checked:border-jade peer-checked:bg-jade-light peer-checked:font-medium peer-checked:text-jade-dark peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-jade peer-[:not(:checked)]:border-tide peer-[:not(:checked)]:bg-white peer-[:not(:checked)]:text-gray-700 peer-[:not(:checked)]:hover:border-gray-300 motion-reduce:transition-none pointer-coarse:h-11"
            >
              {option.label}
            </label>
          </div>
        ))}
      </div>
      {error ? (
        <p id={`${id}-error`} className="flex items-start gap-1 text-xs leading-4 text-danger">
          <CircleAlert aria-hidden className="size-3.5 shrink-0" />
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="text-xs leading-4 text-gray-500">
            {hint}
          </p>
        )
      )}
    </fieldset>
  )
}
