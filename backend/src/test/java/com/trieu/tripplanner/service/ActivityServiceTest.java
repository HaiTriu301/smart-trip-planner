package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.ActivityMapper;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit test: repositories are mocked, the MapStruct mapper is real.
 * Permission is not tested here on purpose: it lives in @PreAuthorize (ActivityControllerTest), never in the
 * service (CLAUDE.md rule 15).
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    private static final long USER_ID = 7L;
    private static final long TRIP_ID = 5L;
    private static final long DAY_ID = 11L;
    private static final long NEW_ACTIVITY_ID = 21L;
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private UserRepository userRepository;

    private ActivityServiceImpl activityService;

    private User creator;
    private Trip trip;
    private TripDay day;

    @BeforeEach
    void setUp() {
        activityService = new ActivityServiceImpl(activityRepository, tripRepository, tripDayRepository,
                userRepository, Mappers.getMapper(ActivityMapper.class));

        creator = TestUsers.verified(USER_ID, "an@example.com");
        trip = Trip.builder()
                .owner(creator)
                .title("Đà Lạt")
                .slug("da-lat-abc123")
                .startDate(OCT_1)
                .endDate(OCT_1)
                .currency("USD")
                .build();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        day = TripDay.builder().trip(trip).dayIndex(1).date(OCT_1).build();
        ReflectionTestUtils.setField(day, "id", DAY_ID);
    }

    @Nested
    class Create {

        @BeforeEach
        void liveTripWithEmptyDay() {
            // lenient: the "not found" tests stop before some of these are reached
            lenient().when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            lenient().when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.of(day));
            lenient().when(userRepository.getReferenceById(USER_ID)).thenReturn(creator);
            lenient().when(activityRepository.findMaxOrderIndexByTripDayId(DAY_ID)).thenReturn(0);
            // What the database does on insert: hand out the id and start the version at 0
            lenient().when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
                Activity activity = invocation.getArgument(0);
                ReflectionTestUtils.setField(activity, "id", NEW_ACTIVITY_ID);
                ReflectionTestUtils.setField(activity, "version", 0L);
                return activity;
            });
        }

        @Test
        void storesEveryFieldWithDayAndCreatorAndReturnsTheResponse() {
            ActivityResponse response = create(new CreateActivityRequest(
                    "  Ăn trưa lẩu gà lá é  ", ActivityType.FOOD, NINE, TEN, "  Đặt bàn trước  ",
                    new BigDecimal("350000.00"), "VND", "https://example.com/booking/123"));

            Activity saved = savedActivity();
            assertThat(saved.getTripDay()).isSameAs(day);
            assertThat(saved.getCreatedBy()).isSameAs(creator);
            assertThat(saved.getTitle()).isEqualTo("Ăn trưa lẩu gà lá é");
            assertThat(saved.getNote()).isEqualTo("Đặt bàn trước");

            assertThat(response).isEqualTo(new ActivityResponse(NEW_ACTIVITY_ID, DAY_ID, "Ăn trưa lẩu gà lá é",
                    ActivityType.FOOD, NINE, TEN, 1000, "Đặt bàn trước", new BigDecimal("350000.00"), "VND",
                    "https://example.com/booking/123", USER_ID, 0L, null, null));
        }

        @Test
        void titleAloneIsEnoughAndTypeDefaultsToOther() {
            create(titled("Dạo phố"));

            Activity saved = savedActivity();
            assertThat(saved.getType()).isEqualTo(ActivityType.OTHER);
            assertThat(saved.getStartTime()).isNull();
            assertThat(saved.getEndTime()).isNull();
            assertThat(saved.getNote()).isNull();
            assertThat(saved.getCostAmount()).isNull();
            assertThat(saved.getCurrency()).isNull();
            assertThat(saved.getBookingUrl()).isNull();
        }

        @Test
        void firstActivityOfTheDayGetsOrderIndex1000() {
            create(titled("Ăn sáng"));

            assertThat(savedActivity().getOrderIndex()).isEqualTo(1000);
        }

        @Test
        void nextActivityGoesAfterTheHighestIndexOfTheDay() {
            // 2500 = an activity was dropped between 2000 and 3000, then 3000 was deleted
            when(activityRepository.findMaxOrderIndexByTripDayId(DAY_ID)).thenReturn(2500);

            create(titled("Chợ đêm"));

            assertThat(savedActivity().getOrderIndex()).isEqualTo(3500);
        }

        @Test
        void costWithoutCurrencyTakesTheCurrencyOfTheTrip() {
            create(withCost(new BigDecimal("12.50"), null));

            assertThat(savedActivity().getCostAmount()).isEqualByComparingTo("12.50");
            assertThat(savedActivity().getCurrency()).isEqualTo("USD");
        }

        @Test
        void costWithItsOwnCurrencyKeepsIt() {
            create(withCost(new BigDecimal("350000"), "VND"));

            assertThat(savedActivity().getCurrency()).isEqualTo("VND");
        }

        @Test
        void blankNoteIsStoredAsNull() {
            create(new CreateActivityRequest("Dạo phố", null, null, null, "   ", null, null, null));

            assertThat(savedActivity().getNote()).isNull();
        }

        @Test
        void secondsSentByTheClientAreDropped() {
            create(timed(LocalTime.of(9, 0, 45), LocalTime.of(10, 30, 59)));

            assertThat(savedActivity().getStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(savedActivity().getEndTime()).isEqualTo(LocalTime.of(10, 30));
        }

        @Test
        void startTimeAloneIsAllowed() {
            create(timed(NINE, null));

            assertThat(savedActivity().getStartTime()).isEqualTo(NINE);
            assertThat(savedActivity().getEndTime()).isNull();
        }

        @Test
        void endOneMinuteAfterStartIsAllowed() {
            create(timed(NINE, NINE.plusMinutes(1)));

            assertThat(savedActivity().getEndTime()).isEqualTo(LocalTime.of(9, 1));
        }

        @Test
        void endTimeWithoutStartTimeIsRejectedOnStartTime() {
            assertThatThrownBy(() -> create(timed(null, TEN)))
                    .satisfies(ex -> assertSingleViolation(ex, "startTime", "error.activity.start-time-required"));
            verify(activityRepository, never()).save(any());
        }

        @Test
        void endEqualToStartIsRejectedOnEndTime() {
            assertThatThrownBy(() -> create(timed(NINE, NINE)))
                    .satisfies(ex -> assertSingleViolation(ex, "endTime", "error.activity.end-not-after-start"));
            verify(activityRepository, never()).save(any());
        }

        @Test
        void endBeforeStartIsRejectedOnEndTime() {
            assertThatThrownBy(() -> create(timed(TEN, NINE)))
                    .satisfies(ex -> assertSingleViolation(ex, "endTime", "error.activity.end-not-after-start"));
            verify(activityRepository, never()).save(any());
        }

        @Test
        void endThatOnlyDiffersBySecondsIsRejectedAfterTruncation() {
            // 09:00:10 → 09:00:50 becomes 09:00 → 09:00 once seconds are dropped
            assertThatThrownBy(() -> create(timed(LocalTime.of(9, 0, 10), LocalTime.of(9, 0, 50))))
                    .satisfies(ex -> assertSingleViolation(ex, "endTime", "error.activity.end-not-after-start"));
        }

        @Test
        void missingOrDeletedTripIsNotFoundAndNothingElseIsTouched() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> create(titled("X")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Trip");
            verifyNoInteractions(tripDayRepository, activityRepository);
        }

        @Test
        void dayOfAnotherTripIsNotFoundAndNothingIsSaved() {
            when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> create(titled("X")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("TripDay");
            verify(activityRepository, never()).save(any());
        }

        // ---- rule 14.4: no overlap inside a day -------------------------------------------------------------

        @Test
        void overlappingActivityIsRejectedWith409NamingTheEarliestConflict() {
            when(activityRepository.findTimedByTripDayId(DAY_ID)).thenReturn(List.of(
                    existing(31L, "Ăn sáng", "09:00", "10:00"),
                    existing(32L, "Cà phê", "10:00", "11:00")));

            assertThatThrownBy(() -> create(timed(LocalTime.of(9, 30), LocalTime.of(10, 30))))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_TIME_CONFLICT);
                        assertThat(ex.getDetails()).containsExactly(FieldViolation.of("startTime",
                                "error.activity.time-conflict-with", "Ăn sáng", "09:00", "10:00"));
                    });
            verify(activityRepository, never()).save(any());
        }

        @ParameterizedTest(name = "{0} to {1} against 09:00 to 10:00, conflict = {2}")
        @CsvSource({
                "09:30, 10:30, true",   // starts inside
                "08:30, 09:30, true",   // ends inside
                "09:15, 09:45, true",   // contained
                "08:00, 11:00, true",   // contains
                "09:00, 10:00, true",   // identical
                "09:59, 10:30, true",   // shares the last minute
                "08:00, 09:01, true",   // shares the first minute
                "10:00, 11:00, false",  // starts when the other ends
                "08:00, 09:00, false",  // ends when the other starts
                "11:00, 12:00, false",  // later the same day
                "03:00, 04:00, false",  // early morning
        })
        void rangesOverlapOnlyWhenTheyShareAMinute(LocalTime start, LocalTime end, boolean conflict) {
            when(activityRepository.findTimedByTripDayId(DAY_ID))
                    .thenReturn(List.of(existing(31L, "Ăn sáng", "09:00", "10:00")));

            if (conflict) {
                assertThatThrownBy(() -> create(timed(start, end)))
                        .isInstanceOfSatisfying(BusinessRuleException.class,
                                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_TIME_CONFLICT));
                verify(activityRepository, never()).save(any());
            } else {
                create(timed(start, end));
                assertThat(savedActivity().getStartTime()).isEqualTo(start);
            }
        }

        @Test
        void secondsAreDroppedBeforeTheOverlapCheck() {
            when(activityRepository.findTimedByTripDayId(DAY_ID))
                    .thenReturn(List.of(existing(31L, "Ăn sáng", "09:00", "10:00")));

            // 10:00:30 becomes 10:00, which only touches the end of 09:00 to 10:00
            create(timed(LocalTime.of(10, 0, 30), LocalTime.of(11, 0)));

            assertThat(savedActivity().getStartTime()).isEqualTo(TEN);
        }

        @Test
        void allowOverlapSkipsTheCheckAndSaves() {
            activityService.create(TRIP_ID, DAY_ID, USER_ID, timed(LocalTime.of(9, 30), LocalTime.of(10, 30)), true);

            assertThat(savedActivity().getStartTime()).isEqualTo(LocalTime.of(9, 30));
            verify(activityRepository, never()).findTimedByTripDayId(anyLong());
        }

        @Test
        void activityWithoutAFullRangeIsNeverChecked() {
            create(titled("Chưa xếp giờ"));
            create(timed(NINE, null));

            verify(activityRepository, never()).findTimedByTripDayId(anyLong());
        }

        private Activity savedActivity() {
            ArgumentCaptor<Activity> saved = ArgumentCaptor.forClass(Activity.class);
            verify(activityRepository, atLeastOnce()).save(saved.capture());
            return saved.getValue();
        }

    }

    /** Every call in this class is the default case of the endpoint: allowOverlap = false. */
    private ActivityResponse create(CreateActivityRequest request) {
        return activityService.create(TRIP_ID, DAY_ID, USER_ID, request, false);
    }

    private Activity existing(long id, String title, String start, String end) {
        Activity activity = Activity.builder().tripDay(day).title(title).orderIndex(1000)
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end)).createdBy(creator).build();
        ReflectionTestUtils.setField(activity, "id", id);
        return activity;
    }

    private static void assertSingleViolation(Throwable ex, String field, String messageKey) {
        assertThat(ex).isInstanceOf(BusinessRuleException.class);
        BusinessRuleException rule = (BusinessRuleException) ex;
        assertThat(rule.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(rule.getDetails()).containsExactly(FieldViolation.of(field, messageKey));
    }

    private static CreateActivityRequest titled(String title) {
        return new CreateActivityRequest(title, null, null, null, null, null, null, null);
    }

    private static CreateActivityRequest timed(LocalTime start, LocalTime end) {
        return new CreateActivityRequest("Tham quan", null, start, end, null, null, null, null);
    }

    private static CreateActivityRequest withCost(BigDecimal cost, String currency) {
        return new CreateActivityRequest("Vé vào cổng", null, null, null, null, cost, currency, null);
    }

}
