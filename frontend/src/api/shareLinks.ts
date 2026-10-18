import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { ShareLink } from '../types/shareLink'

/** Cache key of the links of a trip: its own, so reloading the trip does not ask for them again. */
export const shareLinksKey = (tripId: number) => ['share-links', tripId]

/** Links not revoked, newest first; an expired one is listed with expired = true (owner only, design.md 10.2). */
export async function listShareLinks(tripId: number): Promise<ShareLink[]> {
  const { data } = await apiClient.get<ApiResponse<ShareLink[]>>(`/trips/${tripId}/share-links`)
  return data.data
}
