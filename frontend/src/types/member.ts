// Mirrors backend dto/response/MemberResponse and TripRole for /api/v1/trips/{tripId}/members (design.md 10.2 "Sharing").

/** What a person is to a trip. The owner has no member row: OWNER comes from trips.owner_id (design.md 5.2). */
export type TripRole = 'OWNER' | 'EDITOR' | 'VIEWER'

/** The roles an invitation can give; nobody can be invited as a second owner. */
export type MemberRole = Exclude<TripRole, 'OWNER'>

/** REMOVED rows are never sent to the client. */
export type MemberStatus = 'PENDING' | 'ACCEPTED'

/** One person on a trip: the owner (always first) or an invited member. */
export interface Member {
  /** null for the owner, who has no member row */
  memberId: number | null
  /** null while the invited email has no account yet */
  userId: number | null
  /** null while the invited email has no account yet */
  fullName: string | null
  /** The invited address (lowercase); for the owner, the account email */
  email: string
  avatarUrl: string | null
  role: TripRole
  /** ACCEPTED for the owner */
  status: MemberStatus
  /** When the latest invitation mail was sent; null for the owner */
  invitedAt: string | null
  /** null while pending and for the owner */
  acceptedAt: string | null
}

/** Body of POST /trips/{tripId}/members; inviting an email already pending sends a new mail (design.md 10.2). */
export interface InviteMemberRequest {
  email: string
  role: MemberRole
}

/** Body of PATCH /trips/{tripId}/members/{memberId}; also works on a pending invitation (Task 4.1). */
export interface ChangeMemberRoleRequest {
  role: MemberRole
}
