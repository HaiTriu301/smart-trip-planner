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
  DRAFT: 'bg-gray-100 text-gray-700',
  PLANNED: 'bg-jade-light text-jade-dark',
  ONGOING: 'bg-amber-100 text-amber-800',
  COMPLETED: 'bg-success/12 text-success',
  ARCHIVED: 'bg-gray-200 text-gray-500',
}

export function isTripStatus(value: string | null): value is TripStatus {
  return value !== null && (TRIP_STATUSES as readonly string[]).includes(value)
}
