import type { BadgeTone } from '../../components/badgeStyles'
import type { TripRole } from '../../types/member'

export const TRIP_ROLE_LABELS: Record<TripRole, string> = {
  OWNER: 'Chủ sở hữu',
  EDITOR: 'Chỉnh sửa',
  VIEWER: 'Chỉ xem',
}

/** Badge colour per role (UI_GUIDE 7.5, 15.3) */
export const TRIP_ROLE_TONES: Record<TripRole, BadgeTone> = {
  OWNER: 'brand',
  EDITOR: 'info',
  VIEWER: 'neutral',
}

/**
 * Whether the trip page shows the caller's role next to the title (Task 4.5, decision 4). A member always sees it;
 * the owner only once somebody else is on the list (a pending invitation counts), so a private trip carries no
 * "Chủ sở hữu" label.
 *
 * @param memberCount everyone on the trip, the owner included (the members list of GET /trips/{id})
 */
export function showsRoleBadge(myRole: TripRole, memberCount: number): boolean {
  return myRole !== 'OWNER' || memberCount > 1
}
