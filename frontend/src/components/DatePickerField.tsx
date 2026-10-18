import { lazy, Suspense, useId, useState, type KeyboardEvent, type Ref } from 'react'
import * as Popover from '@radix-ui/react-popover'
import { CalendarDays, ChevronDown } from 'lucide-react'
import { fromCalendarDate, toCalendarDate } from '../lib/calendarDate'
import { formatDate } from '../lib/format'
import { FieldShell } from './FieldShell'
import { controlLookClass, describedBy } from './fieldStyles'
import { useOverlayContainer } from './overlayContainer'
import { Skeleton } from './Skeleton'

// Fetched the first time a calendar opens, like in DateRangeField
const Calendar = lazy(() => import('./Calendar').then((module) => ({ default: module.Calendar })))

interface DatePickerFieldProps {
  label: string
  /** "YYYY-MM-DD" or "" */
  value: string
  onChange: (value: string) => void
  /** Called when the calendar closes, or the field is left without opening it (react-hook-form "touched") */
  onBlur?: () => void
  /** For react-hook-form: setFocus() and the first-error focus land on the button */
  ref?: Ref<HTMLButtonElement>
  /** "YYYY-MM-DD" in the account's time zone (useToday): outlined, and the month shown when nothing is chosen */
  today: string
  /** Days before this one cannot be picked */
  minDate?: string
  /** Opens the calendar as soon as the field appears (after a click on what revealed it) */
  defaultOpen?: boolean
  required?: boolean
  hint?: string
  error?: string
}

// Esc closes the calendar only, not a Modal around (Task 4.9 trap 2)
function keepEscapeInCalendar(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  event.preventDefault()
  event.stopPropagation()
}

/**
 * One day picked on a calendar (UI_GUIDE 7.18, Task 4.9): the single-day sibling of DateRangeField, same field and
 * same floating calendar (one month). A click on a day picks it and closes the calendar; days before minDate are
 * grayed out.
 */
export function DatePickerField({
  label,
  value,
  onChange,
  onBlur,
  ref,
  today,
  minDate,
  defaultOpen = false,
  required,
  hint,
  error,
}: DatePickerFieldProps) {
  const id = useId()
  const container = useOverlayContainer()
  const [open, setOpen] = useState(defaultOpen)
  const firstAllowed = minDate ? toCalendarDate(minDate) : undefined

  function changeOpen(next: boolean) {
    setOpen(next)
    if (!next) onBlur?.()
  }

  return (
    <FieldShell id={id} label={label} required={required} hint={hint} error={error}>
      <Popover.Root open={open} onOpenChange={changeOpen}>
        <Popover.Trigger asChild>
          <button
            ref={ref}
            id={id}
            type="button"
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy(id, error, hint)}
            // Leaving the field without opening it counts as a visit; moving into the calendar does not
            onBlur={() => {
              if (!open) onBlur?.()
            }}
            className={`group flex h-10 w-full cursor-pointer items-center gap-2 px-3 text-left pointer-coarse:h-11 ${controlLookClass(Boolean(error))}`}
          >
            <CalendarDays aria-hidden className="size-4 shrink-0 text-gray-500" />
            <span className={`tabular min-w-0 flex-1 truncate ${value ? '' : 'text-gray-400'}`}>
              {value ? formatDate(value) : 'Chọn ngày'}
            </span>
            <ChevronDown
              aria-hidden
              className="size-4 shrink-0 text-gray-500 transition-transform duration-150 group-data-[state=open]:rotate-180 motion-reduce:transition-none"
            />
          </button>
        </Popover.Trigger>
        <Popover.Portal container={container ?? undefined}>
          <Popover.Content
            align="start"
            sideOffset={4}
            collisionPadding={8}
            // The calendar puts the focus on the chosen day (or today) itself
            onOpenAutoFocus={(event) => event.preventDefault()}
            onKeyDown={keepEscapeInCalendar}
            className="z-50 origin-(--radix-popover-content-transform-origin) rounded-card border border-tide bg-white p-3 text-ink shadow-lg data-[state=closed]:animate-pop-out data-[state=open]:animate-pop-in motion-reduce:animate-none"
          >
            <Suspense fallback={<Skeleton className="h-[296px] w-[280px]" />}>
              <Calendar
                mode="single"
                autoFocus
                today={toCalendarDate(today)}
                defaultMonth={toCalendarDate(value || today)}
                selected={toCalendarDate(value)}
                onSelect={(_selected, triggerDate) => {
                  onChange(fromCalendarDate(triggerDate))
                  changeOpen(false)
                }}
                disabled={firstAllowed ? { before: firstAllowed } : undefined}
              />
            </Suspense>
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
    </FieldShell>
  )
}
