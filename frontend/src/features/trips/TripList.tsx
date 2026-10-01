import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { ChevronLeft, ChevronRight, Plus } from 'lucide-react'
import { getTripStatusCounts, listTrips } from '../../api/trips'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/Alert'
import { Button } from '../../components/Button'
import { EmptyState } from '../../components/EmptyState'
import { controlClass } from '../../components/fieldStyles'
import { LinkButton } from '../../components/LinkButton'
import type { TripListParams, TripStatus, TripStatusCounts } from '../../types/trip'
import { TripCard, TripCardSkeleton } from './TripCard'
import { isTripStatus, TRIP_STATUSES, TRIP_STATUS_LABELS } from './tripStatus'

const PAGE_SIZE = 12
const SKELETON_CARDS = 3

/** Sort choices kept in the URL as short keys; the first one is the backend default and stays out of the URL. */
const SORTS = [
  { key: '', label: 'Tạo gần đây nhất', api: 'createdAt,desc' },
  { key: 'start-asc', label: 'Ngày đi sớm nhất', api: 'startDate,asc' },
  { key: 'start-desc', label: 'Ngày đi muộn nhất', api: 'startDate,desc' },
  { key: 'title', label: 'Tên A → Z', api: 'title,asc' },
] as const

/**
 * Filters live in the URL (?q=&status=&sort=&page=, page is 1-based there) so F5 and the Back button keep them.
 * The keyword is typed in the top bar (TripSearchBox); this list only reads it. The API page is 0-based.
 */
export function TripList() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = searchParams.get('status')
  const q = searchParams.get('q') ?? ''
  const sort = SORTS.find((s) => s.key === searchParams.get('sort')) ?? SORTS[0]
  const page = Math.max(1, Number(searchParams.get('page')) || 1)

  const params: TripListParams = {
    status: isTripStatus(status) ? status : undefined,
    q: q || undefined,
    sort: sort.api,
    page: page - 1,
    size: PAGE_SIZE,
  }

  const { data, error, isPending, isError, isPlaceholderData, refetch } = useQuery({
    queryKey: ['trips', params],
    queryFn: () => listTrips(params),
    // Keep showing the current page while the next one loads, instead of flashing the skeleton
    placeholderData: keepPreviousData,
  })

  // Chip counters follow the keyword but not the chosen status, so every chip keeps its own number.
  // Their key starts with 'trips', so creating, deleting or re-statusing a trip refreshes them too.
  const { data: statusCounts } = useQuery({
    queryKey: ['trips', 'status-counts', params.q ?? ''],
    queryFn: () => getTripStatusCounts(params.q),
    placeholderData: keepPreviousData,
  })

  /** Changing a filter always goes back to page 1; empty values are removed from the URL. */
  function updateParams(changes: Record<string, string>) {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev)
      for (const [key, value] of Object.entries(changes)) {
        if (value) next.set(key, value)
        else next.delete(key)
      }
      if (!('page' in changes)) next.delete('page')
      return next
    })
  }

  const hasFilter = Boolean(params.status || params.q)

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 border-y border-tide py-4 lg:flex-row lg:items-center">
        <StatusChips
          value={params.status}
          counts={statusCounts}
          onChange={(value) => updateParams({ status: value ?? '' })}
        />
        <label className="flex shrink-0 items-center gap-2 text-[13px] text-gray-600 lg:ml-auto">
          Sắp xếp
          <select
            value={sort.key}
            onChange={(e) => updateParams({ sort: e.target.value })}
            className={`h-10 px-3 ${controlClass(false)}`}
          >
            {SORTS.map((s) => (
              <option key={s.key} value={s.key}>
                {s.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {isPending && (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3" role="status" aria-label="Đang tải danh sách chuyến đi">
          {Array.from({ length: SKELETON_CARDS }, (_, i) => (
            <TripCardSkeleton key={i} />
          ))}
        </div>
      )}

      {isError && (
        <div className="space-y-3">
          <Alert variant="error">{getErrorMessage(error)}</Alert>
          <Button variant="secondary" fullWidth={false} onClick={() => refetch()}>
            Thử lại
          </Button>
        </div>
      )}

      {data && data.items.length === 0 &&
        (hasFilter ? (
          <EmptyState
            message="Không tìm thấy chuyến đi nào phù hợp. Thử từ khoá khác hoặc bỏ bớt bộ lọc."
            action={
              <Button variant="secondary" fullWidth={false} onClick={() => updateParams({ q: '', status: '' })}>
                Xoá bộ lọc
              </Button>
            }
          />
        ) : (
          <EmptyState
            message="Chưa có chuyến đi nào. Tạo chuyến đầu tiên để bắt đầu lên lịch trình."
            action={
              <LinkButton to="/trips/new">
                <Plus aria-hidden className="size-4" />
                Tạo chuyến đi
              </LinkButton>
            }
          />
        ))}

      {data && data.items.length > 0 && (
        <>
          <div className={`grid gap-6 sm:grid-cols-2 lg:grid-cols-3 ${isPlaceholderData ? 'opacity-60' : ''}`}>
            {data.items.map((trip) => (
              <TripCard key={trip.id} trip={trip} />
            ))}
          </div>
          {data.totalPages > 1 && (
            <nav className="flex items-center justify-center gap-4" aria-label="Phân trang">
              <Button
                variant="secondary"
                fullWidth={false}
                disabled={page <= 1}
                onClick={() => updateParams({ page: String(page - 1) })}
              >
                <ChevronLeft aria-hidden className="size-4" />
                Trước
              </Button>
              <span className="tabular text-sm text-gray-600">
                Trang {page} / {data.totalPages}
              </span>
              <Button
                variant="secondary"
                fullWidth={false}
                disabled={!data.hasNext}
                onClick={() => updateParams({ page: String(page + 1) })}
              >
                Sau
                <ChevronRight aria-hidden className="size-4" />
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  )
}

interface StatusChipsProps {
  value: TripStatus | undefined
  /** Undefined while the first count loads: chips show no number rather than a wrong 0 */
  counts: TripStatusCounts | undefined
  onChange: (value: TripStatus | undefined) => void
}

/**
 * "Tất cả" + one chip per status, each with its number (Stitch mockup). The chosen chip is filled ink.
 * "Đang diễn ra" carries a jade dot; it stays still: motion only answers an action (UI_GUIDE 2.4).
 * Scrolls sideways on narrow screens.
 */
function StatusChips({ value, counts, onChange }: StatusChipsProps) {
  const chips: { status: TripStatus | undefined; label: string; count: number | undefined }[] = [
    { status: undefined, label: 'Tất cả', count: counts?.total },
    ...TRIP_STATUSES.map((status) => ({ status, label: TRIP_STATUS_LABELS[status], count: counts?.counts[status] })),
  ]
  return (
    <div role="group" aria-label="Lọc theo trạng thái" className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1 lg:pb-0">
      {chips.map((chip) => {
        const active = chip.status === value
        return (
          <button
            key={chip.label}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(chip.status)}
            className={`inline-flex h-9 shrink-0 items-center gap-2 rounded-control px-3.5 text-[13px] font-medium transition-colors focus-visible:ring-[3px] focus-visible:ring-jade/25 focus-visible:outline-none ${
              active ? 'bg-ink text-white' : 'border border-tide bg-white text-gray-600 hover:bg-gray-50'
            }`}
          >
            {chip.status === 'ONGOING' && <span aria-hidden className="size-2 rounded-full bg-jade" />}
            {chip.label}
            {chip.count !== undefined && (
              <span
                className={`tabular min-w-5 rounded-full px-1.5 text-center text-[11px] leading-[18px] ${
                  active ? 'bg-white/20 text-white' : 'bg-gray-100 text-gray-600'
                }`}
              >
                {chip.count}
                <span className="sr-only"> chuyến đi</span>
              </span>
            )}
          </button>
        )
      })}
    </div>
  )
}
