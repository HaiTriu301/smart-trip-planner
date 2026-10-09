package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.AcceptInvitationRequest;
import com.trieu.tripplanner.dto.request.ChangeMemberRoleRequest;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import java.util.List;

/**
 * Members and invitations of a trip (design.md 10.2 "Sharing", rules 14.23–14.25). Permission checks are NOT
 * done here: the controller guards every call with {@code @tripPermission} (CLAUDE.md rule 15), so a method
 * taking a tripId only has to answer 404 when the trip does not exist.
 */
public interface SharingService {

    /**
     * Invites an email to the trip as EDITOR or VIEWER (rule 14.23): stores a PENDING row holding the SHA-256 of
     * a one-time token valid 7 days and mails the invitation link. The email is normalised like at registration;
     * if it already belongs to an account, the row points to it right away.
     * <p>
     * An email that already has a row on the trip is handled on that row (one row per email, decision 11):
     * PENDING → a new token and a new mail (the "resend" of the UI), the role is updated to the one sent;
     * REMOVED → back to PENDING with a new token, the account link is kept; ACCEPTED → 409.
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException   missing or deleted trip (404)
     * @throws com.trieu.tripplanner.exception.BusinessRuleException       the owner's own email, reported on the
     *                                                                     {@code email} field (400)
     * @throws com.trieu.tripplanner.exception.MemberAlreadyExistsException the email has already accepted (409)
     */
    MemberResponse invite(Long tripId, InviteMemberRequest request);

    /**
     * Turns a PENDING invitation into an ACCEPTED membership of the signed-in account (design.md 10.2
     * "Quy ước Sharing API", nhận lời). The link is one-time: the token hash is cleared, so the same link
     * answers 400 afterwards. From here on the permission evaluator lets the account in.
     *
     * @param userId the signed-in account (CLAUDE.md rule 16); its email must be the invited one
     * @throws com.trieu.tripplanner.exception.InvalidTokenException token unknown, expired, already used, or not
     *                                                               an invitation to {@code tripId} (400)
     * @throws com.trieu.tripplanner.exception.ForbiddenException    the account's email is not the invited one
     *                                                               (403): a forwarded link does not work
     */
    MemberResponse accept(Long tripId, Long userId, AcceptInvitationRequest request);

    /**
     * Everybody on the trip (design.md 10.2 "Sharing", GET /members): the owner first (role OWNER, no memberId),
     * then accepted members, then pending invitations, each oldest first. Removed members are not listed.
     * Two queries whatever the number of members.
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException missing or deleted trip (404)
     */
    List<MemberResponse> listMembers(Long tripId);

    /**
     * Sets the role of a member or of a pending invitation (design.md 10.2 "Quy ước Sharing API", đổi vai trò).
     * Takes effect on the member's next request: the evaluator reads the database (Task 4.3 adds a cache and
     * evicts it here). Sending the current role again answers 200 without writing.
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException the trip does not exist, or memberId is
     *                                                                   not a row of this trip, or the member was
     *                                                                   removed (404)
     */
    MemberResponse changeRole(Long tripId, Long memberId, ChangeMemberRoleRequest request);

    /**
     * Removes a member or withdraws a pending invitation (design.md rule 14.25): the row stays with status
     * REMOVED, so inviting the same email again reuses it. The account link and the role are kept as history;
     * the invitation token is cleared, so a pending link dies with the removal. Access ends on the person's next
     * request (the evaluator reads the database; Task 4.3 evicts the cache here). The owner has no row and so
     * can never be removed.
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException the trip does not exist, or memberId is
     *                                                                   not a row of this trip, or the member was
     *                                                                   already removed (404)
     */
    void remove(Long tripId, Long memberId);

}
