import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { PublicTrip } from '../types/publicTrip'

/**
 * The trip behind a share link, for anybody, signed in or not (design.md 10.2 "Trang công khai"). Every successful
 * call adds one view to the link, so the page asks once and never reloads by itself. A token unknown, revoked or
 * expired, or a deleted trip: the same 404.
 */
export async function getPublicTrip(token: string): Promise<PublicTrip> {
  const { data } = await apiClient.get<ApiResponse<PublicTrip>>(`/public/trips/${encodeURIComponent(token)}`)
  return data.data
}
