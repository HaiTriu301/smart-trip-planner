/** What the invitation mail link carries: /invite?trip={tripId}&token={token} (design.md 10.2). */
export interface InviteParams {
  tripId: number
  token: string
}

// The server writes 32 random bytes in URL-safe Base64 (43 characters); a little room either way, so a change
// of length on the server does not break old mails, but nothing that a mail client could have mangled
const TOKEN_PATTERN = /^[A-Za-z0-9_-]{20,128}$/
// A positive whole number that stays exact in JavaScript
const TRIP_ID_PATTERN = /^[1-9]\d{0,14}$/

/**
 * Reads the invitation link, or null when it is not one the server could have sent (a part missing, cut short
 * or mangled by copying). The page then says the invitation is not valid without asking the server.
 */
export function parseInviteParams(search: URLSearchParams): InviteParams | null {
  const trip = search.get('trip')
  const token = search.get('token')
  if (!trip || !TRIP_ID_PATTERN.test(trip) || !token || !TOKEN_PATTERN.test(token)) return null
  return { tripId: Number(trip), token }
}
