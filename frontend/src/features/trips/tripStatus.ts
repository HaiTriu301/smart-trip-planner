import type { TripStatus } from '../../types/trip'

export const TRIP_STATUSES: readonly TripStatus[] = ['DRAFT', 'PLANNED', 'ONGOING', 'COMPLETED', 'ARCHIVED']

export const TRIP_STATUS_LABELS: Record<TripStatus, string> = {
  DRAFT: 'Nháp',
  PLANNED: 'Đã lên kế hoạch',
  ONGOING: 'Đang diễn ra',
  COMPLETED: 'Đã hoàn thành',
  ARCHIVED: 'Đã lưu trữ',
}

export const TRIP_STATUS_STYLES: Record<TripStatus, string> = {
  DRAFT: 'bg-slate-100 text-slate-700',
  PLANNED: 'bg-sky-100 text-sky-800',
  ONGOING: 'bg-amber-100 text-amber-800',
  COMPLETED: 'bg-green-100 text-green-800',
  ARCHIVED: 'bg-slate-200 text-slate-500',
}

export function isTripStatus(value: string | null): value is TripStatus {
  return value !== null && (TRIP_STATUSES as readonly string[]).includes(value)
}
