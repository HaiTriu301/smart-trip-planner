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

/** Body of POST /places/manual; address and category are optional and this app does not ask for them yet. */
export interface ManualPlaceRequest {
  name: string
  lat: number
  lng: number
}

/**
 * A place typed in by the user, for somewhere no source knows. Each call creates a new place (nothing is merged
 * by name) that only its creator may attach to an activity (design rule 14.19).
 */
export async function addManualPlace(body: ManualPlaceRequest): Promise<Place> {
  const { data } = await apiClient.post<ApiResponse<Place>>('/places/manual', body)
  return data.data
}
