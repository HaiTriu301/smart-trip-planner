package com.trieu.tripplanner.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.support.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The three checks against every kind of caller of design.md 6.2. The repository is a mock that answers the
 * access row; the real query is covered by TripRepositoryTest.
 */
@ExtendWith(MockitoExtension.class)
class TripPermissionEvaluatorTest {

    private static final long TRIP_ID = 5L;
    private static final long OWNER_ID = 7L;
    private static final CustomUserDetails OWNER = CustomUserDetails.from(TestUsers.verified(OWNER_ID, "owner@example.com"));
    private static final CustomUserDetails OTHER = CustomUserDetails.from(TestUsers.verified(8L, "other@example.com"));

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripPermissionEvaluator evaluator;

    @Test
    void ownerMayViewEditAndDelete() {
        when(tripRepository.findAccess(TRIP_ID, OWNER_ID)).thenReturn(Optional.of(new TripAccess(OWNER_ID, null)));

        assertThat(evaluator.canView(TRIP_ID, OWNER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OWNER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OWNER)).isTrue();
    }

    @Test
    void acceptedEditorMayViewAndEditButIsNotTheOwner() {
        when(tripRepository.findAccess(TRIP_ID, 8L)).thenReturn(Optional.of(new TripAccess(OWNER_ID, MemberRole.EDITOR)));

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isFalse();
    }

    @Test
    void acceptedViewerMayOnlyView() {
        when(tripRepository.findAccess(TRIP_ID, 8L)).thenReturn(Optional.of(new TripAccess(OWNER_ID, MemberRole.VIEWER)));

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isFalse();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isFalse();
    }

    @Test
    void strangerPendingOrRemovedMemberIsDeniedEverything() {
        // The query gives no role for all three: no row, a PENDING row and a REMOVED row look the same here
        when(tripRepository.findAccess(TRIP_ID, 8L)).thenReturn(Optional.of(new TripAccess(OWNER_ID, null)));

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isFalse();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isFalse();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isFalse();
    }

    @Test
    void missingTripIsLetThroughSoTheServiceAnswers404() {
        when(tripRepository.findAccess(eq(TRIP_ID), anyLong())).thenReturn(Optional.empty());

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isTrue();
    }

    @Test
    void anonymousPrincipalIsDeniedWithoutQuery() {
        assertThat(evaluator.canView(TRIP_ID, "anonymousUser")).isFalse();
        assertThat(evaluator.canEdit(TRIP_ID, null)).isFalse();
        assertThat(evaluator.isOwner(null, OWNER)).isFalse();
        verifyNoInteractions(tripRepository);
    }

}
