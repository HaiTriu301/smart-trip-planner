import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Search } from 'lucide-react'

const SEARCH_DELAY_MS = 300
const TRIPS_PATH = '/trips'

/**
 * Trip search in the top bar (Stitch mockup). On /trips it filters the list as the user types, keeping the
 * other filters (status, sort) and going back to page 1. On any other page, Enter opens /trips?q=...
 * The keyword lives in the URL, so F5, Back and "Xoá bộ lọc" keep the box in step.
 */
export function TripSearchBox({ className }: { className?: string }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const onTripList = pathname === TRIPS_PATH
  const urlValue = onTripList ? (searchParams.get('q') ?? '') : ''

  const [input, setInput] = useState(urlValue)
  const [prevUrlValue, setPrevUrlValue] = useState(urlValue)

  // The URL changed from outside (Back button, "Xoá bộ lọc", leaving the list): show the new text
  if (urlValue !== prevUrlValue) {
    setPrevUrlValue(urlValue)
    setInput(urlValue)
  }

  /** Writes the keyword into the list URL; an empty keyword is removed rather than left as "q=". */
  const applyOnList = useCallback(
    (keyword: string) =>
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          if (keyword) next.set('q', keyword)
          else next.delete('q')
          next.delete('page')
          return next
        },
        { replace: true },
      ),
    [setSearchParams],
  )

  // On the list: wait until typing pauses so each keystroke does not fire a request
  useEffect(() => {
    if (!onTripList) return
    const trimmed = input.trim()
    if (trimmed === urlValue) return
    const timer = setTimeout(() => applyOnList(trimmed), SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [onTripList, input, urlValue, applyOnList])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = input.trim()
    if (onTripList) {
      applyOnList(trimmed)
      return
    }
    navigate(trimmed ? `${TRIPS_PATH}?q=${encodeURIComponent(trimmed)}` : TRIPS_PATH)
  }

  return (
    <form role="search" onSubmit={handleSubmit} className={`relative ${className ?? ''}`}>
      <Search aria-hidden className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-gray-400" />
      <input
        type="search"
        aria-label="Tìm chuyến đi"
        placeholder="Tìm chuyến đi theo tên hoặc điểm đến"
        maxLength={200}
        value={input}
        onChange={(e) => setInput(e.target.value)}
        className="h-10 w-full rounded-control border border-white/15 bg-white/10 pr-3 pl-9 text-[15px] text-white outline-none transition-colors placeholder:text-gray-400 hover:border-white/30 focus:border-jade focus:bg-white/15 focus:ring-[3px] focus:ring-jade/40"
      />
    </form>
  )
}
