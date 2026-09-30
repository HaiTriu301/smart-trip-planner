import type { ActivityType } from '../../types/activity'

export const ACTIVITY_TYPES = [
  'SIGHTSEEING',
  'FOOD',
  'TRANSPORT',
  'ACCOMMODATION',
  'SHOPPING',
  'OTHER',
] as const satisfies readonly ActivityType[]

export const ACTIVITY_TYPE_LABELS: Record<ActivityType, string> = {
  SIGHTSEEING: 'Tham quan',
  FOOD: 'Ăn uống',
  TRANSPORT: 'Di chuyển',
  ACCOMMODATION: 'Lưu trú',
  SHOPPING: 'Mua sắm',
  OTHER: 'Khác',
}

export const ACTIVITY_TYPE_STYLES: Record<ActivityType, string> = {
  SIGHTSEEING: 'bg-emerald-100 text-emerald-800',
  FOOD: 'bg-orange-100 text-orange-800',
  TRANSPORT: 'bg-sky-100 text-sky-800',
  ACCOMMODATION: 'bg-violet-100 text-violet-800',
  SHOPPING: 'bg-pink-100 text-pink-800',
  OTHER: 'bg-slate-100 text-slate-700',
}
