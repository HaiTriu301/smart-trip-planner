import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Place, PlaceResult, PlaceSearchParams } from '../types/place'

/**
 * Places whose name or address matches the keyword, best match first; an empty list when nothing matches.
 * The signal lets a request for an outdated keyword be dropped while the user keeps typing.
 */
export async function searchPlaces(params: PlaceSearchParams, signal?: AbortSignal): Promise<PlaceResult[]> {
  const { data } = await apiClient.get<ApiResponse<PlaceResult[]>>('/places/search', { params, signal })
  return data.data
}

/**
 * "I pick this result": the server reads the place from its source again and stores it, or returns the row it
 * already has (design.md rule 14.18). Only the two values that name the result are sent.
 */
export async function pickPlace(result: Pick<PlaceResult, 'provider' | 'externalId'>): Promise<Place> {
  const { data } = await apiClient.post<ApiResponse<Place>>('/places', {
    provider: result.provider,
    externalId: result.externalId,
  })
  return data.data
}
