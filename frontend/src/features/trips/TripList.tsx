import { useEffect, useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { listTrips } from '../../api/trips'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import type { TripListParams } from '../../types/trip'
import { TripCard } from './TripCard'
import { isTripStatus, TRIP_STATUSES, TRIP_STATUS_LABELS } from './tripStatus'

const PAGE_SIZE = 12
const SEARCH_DELAY_MS = 300

/**
 * Filters live in the URL (?status=&q=&page=, page is 1-based there) so F5 and the Back button keep them.
 * The API page is 0-based.
 */
export function TripList() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = searchParams.get('status')
  const q = searchParams.get('q') ?? ''
  const page = Math.max(1, Number(searchParams.get('page')) || 1)

  const params: TripListParams = {
    status: isTripStatus(status) ? status : undefined,
    q: q || undefined,
    page: page - 1,
    size: PAGE_SIZE,
  }

  const { data, error, isPending, isError, isPlaceholderData, refetch } = useQuery({
    queryKey: ['trips', params],
    queryFn: () => listTrips(params),
    // Keep showing the current page while the next one loads, instead of flashing a spinner
    placeholderData: keepPreviousData,
  })

  /** Changing a filter always goes back to page 1; empty values are removed from the URL. */
  function updateParams(changes: Record<string, string>, replace = false) {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        for (const [key, value] of Object.entries(changes)) {
          if (value) next.set(key, value)
          else next.delete(key)
        }
        if (!('page' in changes)) next.delete('page')
        return next
      },
      { replace },
    )
  }

  const hasFilter = Boolean(params.status || params.q)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row">
        <SearchBox value={q} onSearch={(value) => updateParams({ q: value }, true)} />
        <select
          aria-label="Lọc theo trạng thái"
          value={params.status ?? ''}
          onChange={(e) => updateParams({ status: e.target.value })}
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 shadow-sm outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-200"
        >
          <option value="">Tất cả trạng thái</option>
          {TRIP_STATUSES.map((value) => (
            <option key={value} value={value}>
              {TRIP_STATUS_LABELS[value]}
            </option>
          ))}
        </select>
      </div>

      {isPending && <p className="text-slate-500">Đang tải danh sách chuyến đi...</p>}

      {isError && (
        <div className="space-y-3">
          <Alert variant="error">{getErrorMessage(error)}</Alert>
          <Button variant="secondary" className="w-auto" onClick={() => refetch()}>
            Thử lại
          </Button>
        </div>
      )}

      {data && data.items.length === 0 && (
        <p className="text-slate-500">
          {hasFilter ? 'Không tìm thấy chuyến đi nào phù hợp.' : 'Bạn chưa có chuyến đi nào.'}
        </p>
      )}

      {data && data.items.length > 0 && (
        <>
          <div
            className={`grid gap-4 sm:grid-cols-2 lg:grid-cols-3 ${isPlaceholderData ? 'opacity-60' : ''}`}
          >
            {data.items.map((trip) => (
              <TripCard key={trip.id} trip={trip} />
            ))}
          </div>
          {data.totalPages > 1 && (
            <nav className="flex items-center justify-center gap-4" aria-label="Phân trang">
              <Button
                variant="secondary"
                className="w-auto"
                disabled={page <= 1}
                onClick={() => updateParams({ page: String(page - 1) })}
              >
                Trước
              </Button>
              <span className="text-sm text-slate-600">
                Trang {page} / {data.totalPages}
              </span>
              <Button
                variant="secondary"
                className="w-auto"
                disabled={!data.hasNext}
                onClick={() => updateParams({ page: String(page + 1) })}
              >
                Sau
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  )
}

/** Sends the text to the URL once typing pauses, so each keystroke does not fire a request. */
function SearchBox({ value, onSearch }: { value: string; onSearch: (value: string) => void }) {
  const [input, setInput] = useState(value)
  const [prevValue, setPrevValue] = useState(value)

  // The URL changed from outside (Back button, status filter link): show the new text
  if (value !== prevValue) {
    setPrevValue(value)
    setInput(value)
  }

  useEffect(() => {
    const trimmed = input.trim()
    if (trimmed === value) return
    const timer = setTimeout(() => onSearch(trimmed), SEARCH_DELAY_MS)
    return () => clearTimeout(timer)
  }, [input, value, onSearch])

  return (
    <input
      type="search"
      aria-label="Tìm chuyến đi"
      placeholder="Tìm theo tên hoặc điểm đến"
      maxLength={200}
      value={input}
      onChange={(e) => setInput(e.target.value)}
      className="block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 shadow-sm outline-none focus:border-sky-500 focus:ring-2 focus:ring-sky-200 sm:flex-1"
    />
  )
}
