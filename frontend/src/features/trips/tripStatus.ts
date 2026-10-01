import type { BadgeTone } from '../../components/badgeStyles'
import type { TripStatus } from '../../types/trip'

export const TRIP_STATUSES: readonly TripStatus[] = ['DRAFT', 'PLANNED', 'ONGOING', 'COMPLETED', 'ARCHIVED']

export const TRIP_STATUS_LABELS: Record<TripStatus, string> = {
  DRAFT: 'Nháp',
  PLANNED: 'Đã lên kế hoạch',
  ONGOING: 'Đang diễn ra',
  COMPLETED: 'Đã hoàn thành',
  ARCHIVED: 'Đã lưu trữ',
}

/** Badge colour per status: in progress stands out in the brand colour, archived fades (UI_GUIDE 7.5) */
export const TRIP_STATUS_TONES: Record<TripStatus, BadgeTone> = {
  DRAFT: 'neutral',
  PLANNED: 'info',
  ONGOING: 'brand',
  COMPLETED: 'success',
  ARCHIVED: 'muted',
}

export function isTripStatus(value: string | null): value is TripStatus {
  return value !== null && (TRIP_STATUSES as readonly string[]).includes(value)
}
