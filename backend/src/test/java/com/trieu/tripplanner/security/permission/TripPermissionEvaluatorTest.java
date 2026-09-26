package com.trieu.tripplanner.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.support.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripPermissionEvaluatorTest {

    private static final long TRIP_ID = 5L;
    private static final CustomUserDetails OWNER = CustomUserDetails.from(TestUsers.verified(7L, "owner@example.com"));
    private static final CustomUserDetails STRANGER = CustomUserDetails.from(TestUsers.verified(8L, "other@example.com"));

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripPermissionEvaluator evaluator;

    @Test
    void ownerMayViewEditAndDelete() {
        when(tripRepository.findOwnerIdById(TRIP_ID)).thenReturn(Optional.of(7L));

        assertThat(evaluator.canView(TRIP_ID, OWNER)).isTrue();
        assertThat(evaluator.canEdit(TRIP_ID, OWNER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, OWNER)).isTrue();
    }

    @Test
    void otherUserIsDeniedEverything() {
        when(tripRepository.findOwnerIdById(TRIP_ID)).thenReturn(Optional.of(7L));

        assertThat(evaluator.canView(TRIP_ID, STRANGER)).isFalse();
        assertThat(evaluator.canEdit(TRIP_ID, STRANGER)).isFalse();
        assertThat(evaluator.isOwner(TRIP_ID, STRANGER)).isFalse();
    }

    @Test
    void missingTripIsLetThroughSoTheServiceAnswers404() {
        when(tripRepository.findOwnerIdById(TRIP_ID)).thenReturn(Optional.empty());

        assertThat(evaluator.canView(TRIP_ID, STRANGER)).isTrue();
        assertThat(evaluator.isOwner(TRIP_ID, STRANGER)).isTrue();
    }

    @Test
    void anonymousPrincipalIsDeniedWithoutQuery() {
        assertThat(evaluator.canView(TRIP_ID, "anonymousUser")).isFalse();
        assertThat(evaluator.canEdit(TRIP_ID, null)).isFalse();
        verifyNoInteractions(tripRepository);
    }

}
