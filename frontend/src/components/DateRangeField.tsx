import { lazy, Suspense, useId, useState, type KeyboardEvent, type Ref } from 'react'
import * as Popover from '@radix-ui/react-popover'
import { CalendarDays, ChevronDown } from 'lucide-react'
import { useMediaQuery } from '../hooks/useMediaQuery'
import { addDays, toCalendarDate, fromCalendarDate } from '../lib/calendarDate'
import { formatDate, formatDateRange } from '../lib/format'
import { FieldShell } from './FieldShell'
import { controlLookClass, describedBy } from './fieldStyles'
import { useOverlayContainer } from './overlayContainer'
import { Skeleton } from './Skeleton'

// The calendar library and date-fns are fetched the first time a date field opens, not with every page
const Calendar = lazy(() => import('./Calendar').then((module) => ({ default: module.Calendar })))

export interface DateRange {
  startDate: string
  endDate: string
}

interface DateRangeFieldProps {
  label: string
  /** "YYYY-MM-DD" or "" */
  startDate: string
  endDate: string
  onChange: (range: DateRange) => void
  /** Called when the calendar closes, or the field is left without opening it (react-hook-form "touched") */
  onBlur?: () => void
  /** For react-hook-form: setFocus() and the first-error focus land on the button */
  ref?: Ref<HTMLButtonElement>
  /** "YYYY-MM-DD" in the account's time zone (useToday): outlined, and the month shown when nothing is chosen */
  today: string
  /** The end day cannot be picked further than this many days from the start, both included */
  maxDays?: number
  required?: boolean
  autoFocus?: boolean
  hint?: string
  error?: string
}

// Esc closes the calendar (Radix listens on the document first); stopping it here keeps a Modal around open
// (Task 4.9 trap 2)
function keepEscapeInCalendar(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  event.preventDefault()
  event.stopPropagation()
}

/**
 * The trip's first and last day in one field (UI_GUIDE 7.18, Task 4.9 decision 3). The field shows
 * "12/03/2026 – 16/03/2026"; clicking it opens a calendar (two months from 640px, one below) under it: the first
 * click picks the first day, the second the last one, and the calendar closes. A click before the first day starts
 * again from there; picking the same day twice makes a one-day trip. Past days are allowed (a trip can be logged
 * afterwards); only today is marked. Nothing reaches the form until both days are picked: Esc or a click outside
 * keeps the dates as they were.
 */
export function DateRangeField({
  label,
  startDate,
  endDate,
  onChange,
  onBlur,
  ref,
  today,
  maxDays,
  required,
  autoFocus,
  hint,
  error,
}: DateRangeFieldProps) {
  const id = useId()
  const container = useOverlayContainer()
  const twoMonths = useMediaQuery('(min-width: 640px)')
  const [open, setOpen] = useState(false)
  // The days picked since the calendar opened; it opens on the stored dates
  const [draft, setDraft] = useState<{ from?: string; to?: string }>({})

  function changeOpen(next: boolean) {
    if (next) setDraft({ from: startDate || undefined, to: endDate || undefined })
    setOpen(next)
    if (!next) onBlur?.()
  }

  function pick(day: string) {
    // Nothing started yet, or a full range already: this day starts a new one
    if (!draft.from || draft.to || day < draft.from) {
      setDraft({ from: day })
      return
    }
    onChange({ startDate: draft.from, endDate: day })
    changeOpen(false)
  }

  const picking = Boolean(draft.from && !draft.to)
  const lastAllowed = picking && maxDays ? toCalendarDate(addDays(draft.from as string, maxDays - 1)) : undefined
  const text = startDate && endDate ? formatDateRange(startDate, endDate) : startDate ? `${formatDate(startDate)} – …` : ''

  return (
    <FieldShell id={id} label={label} required={required} hint={hint} error={error}>
      <Popover.Root open={open} onOpenChange={changeOpen}>
        <Popover.Trigger asChild>
          <button
            ref={ref}
            id={id}
            type="button"
            autoFocus={autoFocus}
            aria-invalid={error ? true : undefined}
            aria-describedby={describedBy(id, error, hint)}
            // Leaving the field without opening it counts as a visit; moving into the calendar does not
            onBlur={() => {
              if (!open) onBlur?.()
            }}
            className={`group flex h-10 w-full cursor-pointer items-center gap-2 px-3 text-left pointer-coarse:h-11 ${controlLookClass(Boolean(error))}`}
          >
            <CalendarDays aria-hidden className="size-4 shrink-0 text-gray-500" />
            <span className={`tabular min-w-0 flex-1 truncate ${text ? '' : 'text-gray-400'}`}>
              {text || 'Chọn ngày đi và ngày về'}
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
            <p className="mb-2 px-1 text-[13px] text-gray-600" aria-live="polite">
              {picking ? `Từ ${formatDate(draft.from as string)}: chọn ngày về` : 'Chọn ngày đi'}
            </p>
            {/* Same size as one month while the calendar loads, so the box does not jump */}
            <Suspense fallback={<Skeleton className="h-[296px] w-[280px]" />}>
              <Calendar
                mode="range"
                autoFocus
                numberOfMonths={twoMonths ? 2 : 1}
                today={toCalendarDate(today)}
                defaultMonth={toCalendarDate(draft.from ?? today)}
                selected={{ from: toCalendarDate(draft.from ?? ''), to: toCalendarDate(draft.to ?? '') }}
                onSelect={(_range, triggerDate) => pick(fromCalendarDate(triggerDate))}
                disabled={lastAllowed ? { after: lastAllowed } : undefined}
              />
            </Suspense>
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>
    </FieldShell>
  )
}
