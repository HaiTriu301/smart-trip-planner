import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useQuery } from '@tanstack/react-query'
import { MapPin, Search } from 'lucide-react'
import { searchPlaces } from '../../api/places'
import { getErrorMessage } from '../../api/errors'
import { controlClass } from '../../components/fieldStyles'
import type { Coordinates, PlaceResult } from '../../types/place'
import { ACTIVITY_ROUTE, ACTIVITY_TYPES } from '../itinerary/activityType'

const SEARCH_DELAY_MS = 300
// Same limits as the backend (design.md 10.2 "Quy ước Place API")
const MIN_KEYWORD_LENGTH = 2
const MAX_KEYWORD_LENGTH = 100
/** Rows shown at first; "Xem tất cả" opens the rest */
const SHORT_LIST_SIZE = 5
/** The most one search can return (the backend accepts 1 to 20) */
const SEARCH_LIMIT = 20
// The same keyword gives the same list for a long time (the server keeps it for 24 hours)
const STALE_TIME_MS = 5 * 60_000

interface PlaceSearchFieldProps {
  /** id of the input; the label around this field points to it */
  id: string
  placeholder?: string
  invalid?: boolean
  /** id of the hint or error line under the field */
  describedBy?: string
  disabled?: boolean
  /** Put the cursor in the input when it appears, e.g. after the chosen place was removed */
  focusOnMount?: boolean
  /** Where the user is planning: suggestions around it come first. Null or left out: no preference */
  near?: Coordinates | null
  onPick: (result: PlaceResult) => void
}

/** Icon and colour of a suggestion: those of the activity type its category names, a grey pin otherwise. */
function categoryStyle(category: string | null) {
  const type = ACTIVITY_TYPES.find((t) => t === category)
  return type ? ACTIVITY_ROUTE[type] : { Icon: MapPin, text: 'text-gray-500' }
}

/**
 * Search box with suggestions (UI_GUIDE 7.12). The request leaves 300ms after the last keystroke, from two
 * characters on. The list floats over what is below instead of pushing it down: five rows at first, then
 * "Xem tất cả" opens every result (at most 20) in a box that scrolls. It is driven by the keyboard
 * as a combobox: arrows move, Enter picks, Esc closes the list. Picking only reports the result; what to do
 * with it (store it as a place, or only read its position) is up to the form around.
 */
export function PlaceSearchField({
  id,
  placeholder,
  invalid = false,
  describedBy,
  disabled,
  focusOnMount = false,
  near = null,
  onPick,
}: PlaceSearchFieldProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [text, setText] = useState('')
  const [keyword, setKeyword] = useState('')
  const [listOpen, setListOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(0)
  const [showAll, setShowAll] = useState(false)
  const activeRowRef = useRef<HTMLLIElement>(null)

  useEffect(() => {
    if (focusOnMount) inputRef.current?.focus()
  }, [focusOnMount])

  // The full list scrolls inside its own box: keep the row reached with the arrow keys in view
  useEffect(() => {
    if (showAll) activeRowRef.current?.scrollIntoView({ block: 'nearest' })
  }, [showAll, activeIndex])

  // Wait until typing pauses so each keystroke does not fire a request
  const typed = text.trim()
  useEffect(() => {
    const timer = setTimeout(() => setKeyword(typed), SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [typed])

  const longEnough = typed.length >= MIN_KEYWORD_LENGTH
  const search = useQuery({
    // Not under ['trip', id]: saving an activity must not refetch or cancel a search
    // The point is part of the question: the same keyword near another city gives another order
    queryKey: ['places', 'search', keyword, SEARCH_LIMIT, near?.lat ?? null, near?.lng ?? null],
    queryFn: ({ signal }) =>
      searchPlaces({ q: keyword, limit: SEARCH_LIMIT, lat: near?.lat, lng: near?.lng }, signal),
    enabled: keyword.length >= MIN_KEYWORD_LENGTH,
    staleTime: STALE_TIME_MS,
  })

  // While the pause is running the list on screen belongs to an older keyword: show "searching" instead
  const waiting = typed !== keyword || search.isPending
  const found = waiting || !search.data ? [] : search.data
  // One request brings every result; the short list is only a shorter view of it, so "Xem tất cả" costs no
  // second request
  const hasMore = !showAll && found.length > SHORT_LIST_SIZE
  const results = hasMore ? found.slice(0, SHORT_LIST_SIZE) : found
  // The "Xem tất cả" row sits after the results and is reached with the arrow keys like one of them
  const rowCount = results.length + (hasMore ? 1 : 0)
  const open = listOpen && longEnough
  const active = Math.min(activeIndex, rowCount - 1)
  const onShowAllRow = hasMore && active === results.length
  const listId = `${id}-suggestions`

  function pick(result: PlaceResult) {
    setListOpen(false)
    onPick(result)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter') {
      // Enter in a search box never submits the form around it
      event.preventDefault()
      if (!open) return
      // The row that takes its place is the sixth result: the highlight stays where it was
      if (onShowAllRow) setShowAll(true)
      else if (results[active]) pick(results[active])
      return
    }
    if (event.key === 'Escape' && open) {
      // Close the list only; without this the dialog around would close too
      event.preventDefault()
      event.stopPropagation()
      setListOpen(false)
      return
    }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      if (!open) {
        setListOpen(true)
        return
      }
      if (rowCount === 0) return
      const step = event.key === 'ArrowDown' ? 1 : -1
      setActiveIndex((active + step + rowCount) % rowCount)
    }
  }

  return (
    <div className="relative">
      <Search aria-hidden className="pointer-events-none absolute top-3 left-3 size-4 text-gray-400" />
      <input
        ref={inputRef}
        id={id}
        type="text"
        role="combobox"
        autoComplete="off"
        aria-autocomplete="list"
        aria-expanded={open}
        aria-controls={listId}
        aria-activedescendant={open && rowCount > 0 ? `${id}-option-${active}` : undefined}
        aria-invalid={invalid ? true : undefined}
        aria-describedby={describedBy}
        placeholder={placeholder}
        maxLength={MAX_KEYWORD_LENGTH}
        disabled={disabled}
        value={text}
        onChange={(event) => {
          setText(event.target.value)
          setActiveIndex(0)
          setShowAll(false)
          setListOpen(true)
        }}
        onFocus={() => setListOpen(true)}
        onBlur={() => setListOpen(false)}
        onKeyDown={handleKeyDown}
        className={`h-10 w-full pr-3 pl-9 ${controlClass(invalid)}`}
      />

      {open && (
        <div
          // Keeps the focus in the input while a row is being clicked, so the blur does not close the list first
          onMouseDown={(event) => event.preventDefault()}
          className="absolute top-full right-0 left-0 z-20 mt-1 overflow-hidden rounded-control border border-tide bg-white shadow-md"
        >
          <ul
            id={listId}
            role="listbox"
            aria-label="Gợi ý địa điểm"
            className={showAll ? 'max-h-80 overflow-y-auto overscroll-contain' : undefined}
          >
            {results.map((result, index) => {
              const style = categoryStyle(result.category)
              const isActive = index === active
              return (
                <li
                  key={`${result.provider}:${result.externalId}`}
                  ref={isActive ? activeRowRef : undefined}
                  id={`${id}-option-${index}`}
                  role="option"
                  aria-selected={isActive}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => pick(result)}
                  className={`flex cursor-pointer items-center gap-3 border-l-[3px] px-3 py-2.5 ${
                    index > 0 ? 'border-t border-t-tide' : ''
                  } ${isActive ? 'border-l-jade bg-jade-light' : 'border-l-transparent'}`}
                >
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-gray-100">
                    <style.Icon aria-hidden className={`size-4 ${style.text}`} />
                  </span>
                  <span className="min-w-0">
                    <span className="block truncate text-[15px] leading-5 font-semibold text-ink">{result.name}</span>
                    {result.address && (
                      <span className="block truncate text-[13px] leading-5 text-gray-600">{result.address}</span>
                    )}
                  </span>
                </li>
              )
            })}
            {hasMore && (
              <li
                id={`${id}-option-${results.length}`}
                role="option"
                aria-selected={onShowAllRow}
                onMouseEnter={() => setActiveIndex(results.length)}
                onClick={() => setShowAll(true)}
                className={`cursor-pointer border-t border-l-[3px] border-t-tide px-3 py-2.5 text-[13px] leading-5 font-medium text-jade ${
                  onShowAllRow ? 'border-l-jade bg-jade-light' : 'border-l-transparent'
                }`}
              >
                Xem tất cả {found.length} kết quả
              </li>
            )}
          </ul>
          {showAll && found.length >= SEARCH_LIMIT && (
            <p className="border-t border-tide px-3 py-2 text-xs leading-4 text-gray-500">
              Chỉ hiện {SEARCH_LIMIT} kết quả đầu. Gõ từ khoá cụ thể hơn để thu hẹp.
            </p>
          )}
          {results.length === 0 && (
            <p role="status" className="px-3 py-3 text-[13px] leading-5 text-gray-600">
              {waiting
                ? 'Đang tìm địa điểm…'
                : search.isError
                  ? getErrorMessage(search.error)
                  : 'Không tìm thấy địa điểm nào. Thử từ khoá khác.'}
            </p>
          )}
        </div>
      )}
    </div>
  )
}
