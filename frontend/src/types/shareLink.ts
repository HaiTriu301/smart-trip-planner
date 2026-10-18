// Mirrors backend dto/response/ShareLinkResponse for /api/v1/trips/{tripId}/share-links (design.md 10.2).

/** Phase 4 links only show the trip; editing through a link is for later (design.md 6.2). */
export type SharePermission = 'VIEW'

/** A share link as the owner sees it: only the owner ever receives one, so the token is in it. */
export interface ShareLink {
  id: number
  /** 32 characters [a-z0-9]; the address is {origin}/share/{token}, built by the client */
  token: string
  permission: SharePermission
  /** ISO Instant; null = the link never expires */
  expiresAt: string | null
  /** Successful openings of the public page through this link */
  viewCount: number
  createdAt: string
  /** Worked out by the server clock: still listed, but it opens nothing */
  expired: boolean
}
