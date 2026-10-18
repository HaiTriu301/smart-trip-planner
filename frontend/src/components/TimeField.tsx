import { useEffect, useId, useMemo, useRef, useState, type KeyboardEvent, type Ref } from 'react'
import { Clock } from 'lucide-react'
import { formatDuration, minutesBetween } from '../lib/duration'
import { parseTimeInput, slotIndexFrom, timeSlots } from '../lib/timeInput'
import { FieldShell } from './FieldShell'
import { controlClass, describedBy } from './fieldStyles'

const SLOTS = timeSlots(15)
// Where an empty start field scrolls its list to: the morning, not midnight
const DEFAULT_SCROLL = '08:00'

interface TimeFieldProps {
  label: string
  /** "HH:mm", "" when empty, or the raw text when what was typed is not a time (the schema reports it) */
  value: string
  onChange: (value: string) => void
  onBlur?: () => void
  /** For react-hook-form: setFocus() and the first-error focus land on the input */
  ref?: Ref<HTMLInputElement>
  name?: string
  hint?: string
  error?: string
  /**
   * The start time, for an end field: the list then starts after it and every row says how long the activity
   * lasts ("10:30 (1 giờ 30 phút)"); so does the field once filled.
   */
  durationFrom?: string
}

/**
 * A time typed by hand or picked from a list (UI_GUIDE 7.17, Task 4.9 decision 4), always 24-hour, unlike
 * <input type="time"> whose look follows the computer's settings. Typing "9", "930" or "21h" becomes 09:00, 09:30,
 * 21:00 when the field is left or Enter is pressed. Clicking the field, or ↓, opens the list of times every 15
 * minutes, floating under it like the place search (UI_GUIDE 7.12): ↑ ↓ move, Enter picks, Esc closes the list
 * only, and the typed text moves the highlight to the matching time. The focus never leaves the input.
 */
export function TimeField({ label, value, onChange, onBlur, ref, name, hint, error, durationFrom }: TimeFieldProps) {
  const id = useId()
  const listId = `${id}-list`
  const listRef = useRef<HTMLUListElement>(null)
  // Text being typed; null while the field just shows the form value
  const [draft, setDraft] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState<number | null>(null)

  const start = durationFrom && parseTimeInput(durationFrom)
  // An end field lists the times after the start only
  const slots = useMemo(() => (start ? SLOTS.filter((slot) => slot > start) : SLOTS), [start])
  const shown = draft ?? value

  // The row that matches what is typed, or the stored value: highlighted and scrolled into view
  const typed = parseTimeInput(shown)
  const matchIndex = typed ? slotIndexFrom(slots, typed) : -1
  const highlighted = active ?? (matchIndex >= 0 ? matchIndex : null)

  // Scroll the list itself (not the dialog around it) so the highlighted row, or 08:00, sits in the middle
  useEffect(() => {
    const list = listRef.current
    if (!open || !list) return
    const index = highlighted ?? (start ? 0 : slotIndexFrom(slots, DEFAULT_SCROLL))
    const row = list.children[index] as HTMLElement | undefined
    if (row) list.scrollTop = row.offsetTop - list.clientHeight / 2 + row.clientHeight / 2
  }, [open, highlighted, slots, start])

  function commit(text: string) {
    const parsed = parseTimeInput(text)
    // Not a time: keep the text, so the person sees what they typed next to the error of the schema
    onChange(parsed ?? text.trim())
    setDraft(null)
  }

  function pick(slot: string) {
    onChange(slot)
    setDraft(null)
    setOpen(false)
    setActive(null)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      if (!open) {
        setOpen(true)
        return
      }
      const from = highlighted ?? (event.key === 'ArrowDown' ? -1 : slots.length)
      const next = event.key === 'ArrowDown' ? Math.min(from + 1, slots.length - 1) : Math.max(from - 1, 0)
      setActive(next)
      return
    }
    if (event.key === 'Enter') {
      if (open && highlighted !== null && slots[highlighted]) {
        // Picks the row; the form is not sent
        event.preventDefault()
        pick(slots[highlighted])
        return
      }
      // Closed list: the typed text becomes a time before the form reads it (Enter then sends the form)
      if (draft !== null) commit(draft)
      setOpen(false)
      return
    }
    if (event.key === 'Escape' && open) {
      // Close the list only; without this the dialog around would close too (UI_GUIDE 7.6)
      event.preventDefault()
      event.stopPropagation()
      setOpen(false)
      setActive(null)
    }
  }

  const minutes = start && typed ? minutesBetween(start, typed) : null
  const durationText = minutes ? `(${formatDuration(minutes)})` : null

  return (
    <FieldShell id={id} label={label} hint={hint} error={error}>
      <div className="relative">
        <Clock aria-hidden className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-gray-400" />
        <input
          ref={ref}
          id={id}
          name={name}
          type="text"
          inputMode="numeric"
          autoComplete="off"
          placeholder="--:--"
          role="combobox"
          aria-expanded={open}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={open && highlighted !== null ? `${id}-opt-${highlighted}` : undefined}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(id, error, hint)}
          value={shown}
          onFocus={() => setDraft(value)}
          onClick={() => setOpen(true)}
          onChange={(event) => {
            setDraft(event.target.value)
            setActive(null)
          }}
          onKeyDown={handleKeyDown}
          onBlur={() => {
            if (draft !== null) commit(draft)
            setOpen(false)
            setActive(null)
            onBlur?.()
          }}
          // The duration sits inside the field on the right: room for it is kept only while it shows
          className={`tabular h-10 w-full pl-9 pointer-coarse:h-11 ${durationText ? 'pr-32' : 'pr-3'} ${controlClass(Boolean(error))}`}
        />
        {durationText && (
          <span className="tabular pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[13px] text-gray-500">
            {durationText}
          </span>
        )}
        {open && slots.length > 0 && (
          <ul
            ref={listRef}
            id={listId}
            role="listbox"
            aria-label={label}
            className="absolute top-full right-0 left-0 z-20 mt-1 max-h-60 origin-top animate-pop-in overflow-y-auto rounded-control border border-tide bg-white p-1 shadow-md motion-reduce:animate-none"
          >
            {slots.map((slot, index) => {
              const rowMinutes = start ? minutesBetween(start, slot) : null
              return (
                <li
                  key={slot}
                  id={`${id}-opt-${index}`}
                  role="option"
                  aria-selected={index === highlighted}
                  // Keep the focus in the input: its blur would close the list before the click lands
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => pick(slot)}
                  className={`tabular flex cursor-pointer items-center justify-between gap-3 rounded-[4px] px-2.5 py-1.5 text-[15px] pointer-coarse:py-2.5 ${index === highlighted ? 'bg-jade-light' : 'hover:bg-gray-100'}`}
                >
                  {slot}
                  {rowMinutes && <span className="text-[13px] text-gray-500">{formatDuration(rowMinutes)}</span>}
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </FieldShell>
  )
}
