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
  /** the type chip on the activity card: light tint of the colour, label in the colour */
  chip: string
  /** ring of the 10px dot on the rail */
  dot: string
  /** icon and label colour */
  text: string
  /** fill of the teardrop marker on the map */
  marker: string
  /** the chosen button of the type picker: 2px outline (border + inset ring), light tint, coloured label */
  chosen: string
  Icon: LucideIcon
}

/**
 * Route colour of each type (UI_GUIDE 3.4): used only on the rail dot, the type chip of the card, the chosen
 * button of the type picker and the map marker. Never as a card background. The icon + label keep the type
 * readable without colour.
 * Class names are written out in full so Tailwind finds them.
 */
export const ACTIVITY_ROUTE: Record<ActivityType, RouteStyle> = {
  SIGHTSEEING: {
    chip: 'bg-act-sightseeing/12 text-act-sightseeing',
    dot: 'border-act-sightseeing',
    text: 'text-act-sightseeing',
    marker: 'bg-act-sightseeing',
    chosen:
      'peer-checked:border-act-sightseeing peer-checked:bg-act-sightseeing/8 peer-checked:font-medium peer-checked:text-act-sightseeing peer-checked:ring-1 peer-checked:ring-inset peer-checked:ring-act-sightseeing',
    Icon: Landmark,
  },
  FOOD: {
    chip: 'bg-act-food/12 text-act-food',
    dot: 'border-act-food',
    text: 'text-act-food',
    marker: 'bg-act-food',
    chosen:
      'peer-checked:border-act-food peer-checked:bg-act-food/8 peer-checked:font-medium peer-checked:text-act-food peer-checked:ring-1 peer-checked:ring-inset peer-checked:ring-act-food',
    Icon: Utensils,
  },
  TRANSPORT: {
    chip: 'bg-act-transport/12 text-act-transport',
    dot: 'border-act-transport',
    text: 'text-act-transport',
    marker: 'bg-act-transport',
    chosen:
      'peer-checked:border-act-transport peer-checked:bg-act-transport/8 peer-checked:font-medium peer-checked:text-act-transport peer-checked:ring-1 peer-checked:ring-inset peer-checked:ring-act-transport',
    Icon: Bus,
  },
  ACCOMMODATION: {
    chip: 'bg-act-accommodation/12 text-act-accommodation',
    dot: 'border-act-accommodation',
    text: 'text-act-accommodation',
    marker: 'bg-act-accommodation',
    chosen:
      'peer-checked:border-act-accommodation peer-checked:bg-act-accommodation/8 peer-checked:font-medium peer-checked:text-act-accommodation peer-checked:ring-1 peer-checked:ring-inset peer-checked:ring-act-accommodation',
    Icon: BedDouble,
  },
  SHOPPING: {
    chip: 'bg-act-shopping/12 text-act-shopping',
    dot: 'border-act-shopping',
    text: 'text-act-shopping',
    marker: 'bg-act-shopping',
    chosen:
      'peer-checked:border-act-shopping peer-checked:bg-act-shopping/8 peer-checked:font-medium peer-checked:text-act-shopping peer-checked:ring-1 peer-checked:ring-inset peer-checked:ring-act-shopping',
    Icon: ShoppingBag,
  },
  OTHER: {
    chip: 'bg-act-other/12 text-act-other',
    dot: 'border-act-other',
    text: 'text-act-other',
    marker: 'bg-act-other',
    chosen:
      'peer-checked:border-act-other peer-checked:bg-act-other/8 peer-checked:font-medium peer-checked:text-act-other peer-checked:ring-1 peer-checked:ring-inset peer-checked:ring-act-other',
    Icon: MapPin,
  },
}
