import { useId } from 'react'
import { CircleAlert } from 'lucide-react'
import type { UseFormRegisterReturn } from 'react-hook-form'
import { ACTIVITY_ROUTE, ACTIVITY_TYPES, ACTIVITY_TYPE_LABELS } from './activityType'

interface ActivityTypeFieldProps {
  /** register('type') of the form: every radio shares it */
  registration: UseFormRegisterReturn<'type'>
  error?: string
}

/**
 * "Loại" as six buttons instead of a select (UI_GUIDE 7.13): every type is visible with its icon, and one click
 * picks it. They are real radio inputs, hidden behind their labels: Tab reaches the group, the arrow keys move
 * inside it, and the form reads the value like any other field.
 */
export function ActivityTypeField({ registration, error }: ActivityTypeFieldProps) {
  const id = useId()

  return (
    <fieldset aria-describedby={error ? `${id}-error` : undefined} className="space-y-1.5">
      <legend className="mb-1.5 block text-[13px] leading-[18px] font-medium text-gray-700">Loại</legend>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
        {ACTIVITY_TYPES.map((type) => {
          const route = ACTIVITY_ROUTE[type]
          return (
            <div key={type} className="relative">
              <input type="radio" id={`${id}-${type}`} value={type} className="peer sr-only" {...registration} />
              {/* The hover border applies to the buttons that are not chosen only: on the chosen one it would
                  compete with the route colour, with no guaranteed winner (BUG-UI-001) */}
              <label
                htmlFor={`${id}-${type}`}
                className={`flex h-11 cursor-pointer items-center justify-center gap-2 rounded-control border border-tide bg-white px-2 text-[15px] text-ink transition-colors peer-[:not(:checked)]:hover:border-gray-300 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-jade sm:h-10 ${route.chosen}`}
              >
                <route.Icon aria-hidden className={`size-4 shrink-0 ${route.text}`} />
                <span className="truncate">{ACTIVITY_TYPE_LABELS[type]}</span>
              </label>
            </div>
          )
        })}
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
