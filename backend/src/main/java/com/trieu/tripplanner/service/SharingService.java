package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;

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
     *
     * @throws com.trieu.tripplanner.exception.ResourceNotFoundException missing or deleted trip (404)
     * @throws com.trieu.tripplanner.exception.BusinessRuleException     the owner's own email, reported on the
     *                                                                   {@code email} field (400)
     */
    MemberResponse invite(Long tripId, InviteMemberRequest request);

}
