import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { InviteMemberRequest, Member } from '../types/member'

/** Names the invite mutation, so the sharing dialog can tell an invitation is being sent and stay open. */
export const inviteMemberKey = (tripId: number) => ['invite-member', tripId]

/**
 * Invites an email to the trip and sends the invitation mail (owner only, design.md 10.2). 201 with the new
 * member; an email already pending gets a new mail. 400 on the owner's own email, 409 MEMBER_ALREADY_EXISTS
 * when the person has already accepted.
 */
export async function inviteMember(tripId: number, body: InviteMemberRequest): Promise<Member> {
  const { data } = await apiClient.post<ApiResponse<Member>>(`/trips/${tripId}/members`, body)
  return data.data
}
