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

/** Names the create mutation, so the sharing dialog stays open until the server has answered. */
export const createShareLinkKey = (tripId: number) => ['create-share-link', tripId]

/**
 * Creates a view-only link (owner only). expiresAt is optional, an ISO Instant in the future; without it the link
 * never expires. 400 VALIDATION_ERROR on field expiresAt when it is not ahead of the server clock (design.md 10.2).
 */
export async function createShareLink(tripId: number, expiresAt: string | undefined): Promise<ShareLink> {
  const { data } = await apiClient.post<ApiResponse<ShareLink>>(`/trips/${tripId}/share-links`, { expiresAt })
  return data.data
}

/**
 * Revokes a link (owner only): the public page answers 404 from now on, and revoking cannot be undone. The last
 * link revoked turns the trip back to PRIVATE. 404 when the link is not on this trip or already revoked.
 */
export async function revokeShareLink(tripId: number, linkId: number): Promise<void> {
  await apiClient.delete(`/trips/${tripId}/share-links/${linkId}`)
}
