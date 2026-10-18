import { useEffect, useRef } from 'react'
import { DayPicker, type ChevronProps, type DayButtonProps, type DayPickerProps } from 'react-day-picker'
import { vi } from 'react-day-picker/locale'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { fromCalendarDate } from '../lib/calendarDate'

// Vietnamese headers as the mockup has them: "Tháng 3 2026" and "T2 … CN" (date-fns writes "Tháng Ba", "Th 2")
const formatters: DayPickerProps['formatters'] = {
  formatCaption: (month) => `Tháng ${month.getMonth() + 1} ${month.getFullYear()}`,
  formatWeekdayName: (weekday) => (weekday.getDay() === 0 ? 'CN' : `T${weekday.getDay() + 1}`),
}

// The library's own stylesheet is not loaded (Task 4.9 trap 6): every part gets token classes here. Days are 40px
// (44px for a finger). The cell carries the light teal band of a range; the round day button on top of it is
// styled from the modifiers in DayButton below, one class set at a time.
const classNames: DayPickerProps['classNames'] = {
  root: 'w-fit',
  months: 'relative flex flex-col gap-6 sm:flex-row',
  month: 'flex flex-col gap-3',
  month_caption: 'flex h-8 items-center justify-center',
  caption_label: 'text-[15px] font-semibold text-ink',
  nav: 'absolute inset-x-0 top-0 flex h-8 items-center justify-between',
  button_previous:
    'flex size-8 cursor-pointer items-center justify-center rounded-control text-gray-600 outline-none hover:bg-gray-100 hover:text-ink focus-visible:ring-[3px] focus-visible:ring-jade/25 disabled:cursor-default disabled:opacity-40 pointer-coarse:size-11',
  button_next:
    'flex size-8 cursor-pointer items-center justify-center rounded-control text-gray-600 outline-none hover:bg-gray-100 hover:text-ink focus-visible:ring-[3px] focus-visible:ring-jade/25 disabled:cursor-default disabled:opacity-40 pointer-coarse:size-11',
  month_grid: 'border-collapse',
  weekday: 'size-10 text-xs font-medium text-gray-500 pointer-coarse:size-11',
  day: 'size-10 p-0 text-center pointer-coarse:size-11',
  range_start: 'rounded-l-full bg-jade-light',
  range_middle: 'bg-jade-light',
  range_end: 'rounded-r-full bg-jade-light',
  // Left to DayButton: the default class names would only add the library's own (unloaded) styles
  selected: '',
  today: '',
  disabled: '',
  outside: '',
  hidden: 'invisible',
}

function Chevron({ orientation }: ChevronProps) {
  return orientation === 'left' ? (
    <ChevronLeft aria-hidden className="size-4" />
  ) : (
    <ChevronRight aria-hidden className="size-4" />
  )
}

/**
 * One day: a 40px circle. Chosen (a single date, or either end of a range): solid jade, white text. Inside a range:
 * no fill, the cell's band shows through. Today: a jade ring. Not allowed: gray, no hover. Only one of these class
 * sets applies, so none overrides another (BUG-UI-001). Focus is moved here by the library through the "focused"
 * modifier, as in its own DayButton.
 */
function DayButton({ day, modifiers, className, ...buttonProps }: DayButtonProps) {
  const ref = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (modifiers.focused) ref.current?.focus()
  }, [modifiers.focused])

  const chosen = modifiers.range_start || modifiers.range_end || (modifiers.selected && !modifiers.range_middle)
  const look = modifiers.disabled
    ? 'cursor-not-allowed text-gray-300'
    : chosen
      ? 'bg-jade font-semibold text-white'
      : modifiers.range_middle
        ? 'text-jade-dark hover:bg-jade/10'
        : modifiers.today
          ? 'font-semibold text-jade-dark ring-1 ring-jade ring-inset hover:bg-gray-100'
          : 'text-ink hover:bg-gray-100'

  return (
    <button
      ref={ref}
      // The day as the form stores it, handy to find a cell in DevTools
      data-day={fromCalendarDate(day.date)}
      className={`tabular flex size-10 cursor-pointer items-center justify-center rounded-full text-sm outline-none transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/40 pointer-coarse:size-11 ${look} ${className ?? ''}`}
      {...buttonProps}
    />
  )
}

/**
 * A month calendar in Vietnamese on react-day-picker (UI_GUIDE 7.18, Task 4.9; the shadcn/ui Calendar was the
 * template, rewritten with the project's tokens). Weeks start on Monday. Takes every prop of DayPicker (mode,
 * selected, onSelect, numberOfMonths, disabled...); days outside the shown month are hidden.
 */
export function Calendar(props: DayPickerProps) {
  return (
    <DayPicker
      locale={vi}
      showOutsideDays={false}
      formatters={formatters}
      classNames={classNames}
      components={{ Chevron, DayButton }}
      {...props}
    />
  )
}
