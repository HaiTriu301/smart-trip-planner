package com.trieu.tripplanner.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.support.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The three checks against every kind of caller of design.md 6.2.
 * The access row comes from a mock of TripAccessCache; the real query is covered by TripRepositoryTest, the real
 * cache by TripAccessCacheIntegrationTest. A refusal is always read again ({@code refresh}) before it is final,
 * so the refusing cases answer the same row from both methods unless they test what the second read changes.
 */
@ExtendWith(MockitoExtension.class)
class TripPermissionEvaluatorTest {

    private static final long TRIP_ID = 5L;
    private static final long OWNER_ID = 7L;
    private static final long OTHER_ID = 8L;
    private static final CustomUserDetails OWNER = CustomUserDetails.from(TestUsers.verified(OWNER_ID, "owner@example.com"));
    private static final CustomUserDetails OTHER = CustomUserDetails.from(TestUsers.verified(OTHER_ID, "other@example.com"));

    private static final TripAccess STRANGER_ROW = new TripAccess(OWNER_ID, null);

    @Mock
    private TripAccessCache tripAccessCache;

    @InjectMocks
    private TripPermissionEvaluator evaluator;

    @Test
    void ownerMayViewEditAndDeleteFromTheStoredRowAlone() {
        when(tripAccessCache.find(TRIP_ID, OWNER_ID)).thenReturn(Optional.of(new TripAccess(OWNER_ID, null)));

        assertThat(evaluator.canView(TRIP_ID, OWNER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OWNER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OWNER)).isTrue();
        // Let in: nothing is read again
        verify(tripAccessCache, never()).refresh(anyLong(), anyLong());
    }

    @Test
    void acceptedEditorMayViewAndEditButIsNotTheOwner() {
        sameRowStoredAndRead(new TripAccess(OWNER_ID, MemberRole.EDITOR));

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isFalse();
    }

    @Test
    void acceptedViewerMayOnlyView() {
        sameRowStoredAndRead(new TripAccess(OWNER_ID, MemberRole.VIEWER));

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isFalse();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isFalse();
    }

    @Test
    void strangerPendingOrRemovedMemberIsDeniedEverything() {
        // The query gives no role for all three: no row, a PENDING row and a REMOVED row look the same here
        sameRowStoredAndRead(STRANGER_ROW);

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isFalse();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isFalse();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isFalse();
    }

    @Test
    void missingTripIsLetThroughSoTheServiceAnswers404() {
        when(tripAccessCache.find(eq(TRIP_ID), anyLong())).thenReturn(Optional.empty());

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isTrue();
        verify(tripAccessCache, never()).refresh(anyLong(), anyLong());
    }

    @Test
    void storedRefusalOfATripDeletedSinceIsLetThroughSoTheServiceAnswers404() {
        // BUG-SHARE-002: "stranger" was stored, then the owner deleted the trip
        when(tripAccessCache.find(TRIP_ID, OTHER_ID)).thenReturn(Optional.of(STRANGER_ROW));
        when(tripAccessCache.refresh(TRIP_ID, OTHER_ID)).thenReturn(Optional.empty());

        assertThat(evaluator.canView(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OTHER)).isTrue();
    }

    @Test
    void storedRefusalIsOverruledByWhatTheDatabaseSaysNow() {
        // A viewer stored, promoted since: the edit is allowed on the fresh row
        when(tripAccessCache.find(TRIP_ID, OTHER_ID)).thenReturn(Optional.of(new TripAccess(OWNER_ID, MemberRole.VIEWER)));
        when(tripAccessCache.refresh(TRIP_ID, OTHER_ID)).thenReturn(Optional.of(new TripAccess(OWNER_ID, MemberRole.EDITOR)));

        assertThat(evaluator.canEdit(TRIP_ID, OTHER)).isTrue();
    }

    @Test
    void anonymousPrincipalIsDeniedWithoutQuery() {
        assertThat(evaluator.canView(TRIP_ID, "anonymousUser")).isFalse();
        assertThat(evaluator.canEdit(TRIP_ID, null)).isFalse();
        assertThat(evaluator.isOwner(null, OWNER)).isFalse();
        verifyNoInteractions(tripAccessCache);
    }

    /** Stored and read again, the database gives the same row: what the caller is did not change. */
    private void sameRowStoredAndRead(TripAccess row) {
        when(tripAccessCache.find(TRIP_ID, OTHER_ID)).thenReturn(Optional.of(row));
        when(tripAccessCache.refresh(TRIP_ID, OTHER_ID)).thenReturn(Optional.of(row));
    }

}
