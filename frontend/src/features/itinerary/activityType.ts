import { BedDouble, Bus, Landmark, MapPin, ShoppingBag, Utensils, type LucideIcon } from 'lucide-react'
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

interface RouteStyle {
  /** 3px left edge of the card */
  edge: string
  /** ring of the 10px dot on the rail */
  dot: string
  /** icon and label colour */
  text: string
  Icon: LucideIcon
}

/**
 * Route colour of each type (UI_GUIDE 3.4): used only on the rail dot, the card's left edge and (Phase 3) the
 * map marker. Never as a card background. The icon + label keep the type readable without colour.
 * Class names are written out in full so Tailwind finds them.
 */
export const ACTIVITY_ROUTE: Record<ActivityType, RouteStyle> = {
  SIGHTSEEING: { edge: 'border-l-act-sightseeing', dot: 'border-act-sightseeing', text: 'text-act-sightseeing', Icon: Landmark },
  FOOD: { edge: 'border-l-act-food', dot: 'border-act-food', text: 'text-act-food', Icon: Utensils },
  TRANSPORT: { edge: 'border-l-act-transport', dot: 'border-act-transport', text: 'text-act-transport', Icon: Bus },
  ACCOMMODATION: {
    edge: 'border-l-act-accommodation',
    dot: 'border-act-accommodation',
    text: 'text-act-accommodation',
    Icon: BedDouble,
  },
  SHOPPING: { edge: 'border-l-act-shopping', dot: 'border-act-shopping', text: 'text-act-shopping', Icon: ShoppingBag },
  OTHER: { edge: 'border-l-act-other', dot: 'border-act-other', text: 'text-act-other', Icon: MapPin },
}
