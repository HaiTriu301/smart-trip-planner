package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.request.ReorderActivitiesRequest;
import com.trieu.tripplanner.dto.request.UpdateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.dto.response.TripDayDetailResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.ActivityMapper;
import com.trieu.tripplanner.mapper.TripDayMapper;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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
                userRepository, Mappers.getMapper(ActivityMapper.class), Mappers.getMapper(TripDayMapper.class));

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
    class ListOfDay {

        @Test
        void returnsTheActivitiesOfTheDayInRepositoryOrder() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.of(day));
            when(activityRepository.findByTripDayIdOrderByOrderIndexAscIdAsc(DAY_ID)).thenReturn(List.of(
                    existing(31L, "Ăn sáng", "09:00", "10:00"),
                    existing(32L, "Cà phê", "10:00", "11:00")));

            List<ActivityResponse> responses = activityService.list(TRIP_ID, DAY_ID);

            assertThat(responses)
                    .extracting(ActivityResponse::id, ActivityResponse::dayId, ActivityResponse::title,
                            ActivityResponse::startTime, ActivityResponse::createdById)
                    .containsExactly(
                            tuple(31L, DAY_ID, "Ăn sáng", NINE, USER_ID),
                            tuple(32L, DAY_ID, "Cà phê", TEN, USER_ID));
        }

        @Test
        void dayWithoutActivitiesGivesAnEmptyList() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.of(day));

            assertThat(activityService.list(TRIP_ID, DAY_ID)).isEmpty();
        }

        @Test
        void missingOrDeletedTripIsNotFoundAndNothingElseIsQueried() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(false);

            assertThatThrownBy(() -> activityService.list(TRIP_ID, DAY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Trip");
            verifyNoInteractions(tripDayRepository, activityRepository);
        }

        @Test
        void dayOfAnotherTripIsNotFoundAndActivitiesAreNotQueried() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.list(TRIP_ID, DAY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("TripDay");
            verifyNoInteractions(activityRepository);
        }

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

    @Nested
    class Update {

        private static final long ACTIVITY_ID = 31L;
        private static final LocalTime TEN_THIRTY = LocalTime.of(10, 30);

        private Activity stored;

        @BeforeEach
        void storedActivity() {
            stored = Activity.builder()
                    .tripDay(day)
                    .title("Ăn sáng")
                    .type(ActivityType.FOOD)
                    .startTime(NINE)
                    .endTime(TEN)
                    .orderIndex(2000)
                    .note("Ghi chú cũ")
                    .costAmount(new BigDecimal("50000.00"))
                    .currency("VND")
                    .bookingUrl("https://example.com/old")
                    .createdBy(creator)
                    .build();
            ReflectionTestUtils.setField(stored, "id", ACTIVITY_ID);
            ReflectionTestUtils.setField(stored, "version", 0L);

            // lenient: the "not found" tests stop before some of these are reached
            lenient().when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
            lenient().when(activityRepository.findByIdAndTripId(ACTIVITY_ID, TRIP_ID)).thenReturn(Optional.of(stored));
            lenient().when(activityRepository.saveAndFlush(any(Activity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void changesOnlyTheFieldsSentAndNeverTheDayThePositionOrTheCreator() {
            ActivityResponse response = update(titled("  Ăn sáng muộn  "));

            assertThat(stored.getTitle()).isEqualTo("Ăn sáng muộn");
            assertThat(stored.getType()).isEqualTo(ActivityType.FOOD);
            assertThat(stored.getStartTime()).isEqualTo(NINE);
            assertThat(stored.getEndTime()).isEqualTo(TEN);
            assertThat(stored.getNote()).isEqualTo("Ghi chú cũ");
            assertThat(stored.getCostAmount()).isEqualByComparingTo("50000");
            assertThat(stored.getCurrency()).isEqualTo("VND");
            assertThat(stored.getBookingUrl()).isEqualTo("https://example.com/old");
            assertThat(stored.getOrderIndex()).isEqualTo(2000);
            assertThat(stored.getTripDay()).isSameAs(day);
            assertThat(stored.getCreatedBy()).isSameAs(creator);

            verify(activityRepository).saveAndFlush(stored);
            assertThat(response.id()).isEqualTo(ACTIVITY_ID);
            assertThat(response.title()).isEqualTo("Ăn sáng muộn");
        }

        @Test
        void everyEditableFieldCanBeChanged() {
            update(new UpdateActivityRequest("Ăn trưa", ActivityType.SHOPPING, LocalTime.of(11, 30),
                    LocalTime.of(13, 0), "  Ghi chú mới  ", new BigDecimal("12.50"), "USD",
                    "https://example.com/new"));

            assertThat(stored.getTitle()).isEqualTo("Ăn trưa");
            assertThat(stored.getType()).isEqualTo(ActivityType.SHOPPING);
            assertThat(stored.getStartTime()).isEqualTo(LocalTime.of(11, 30));
            assertThat(stored.getEndTime()).isEqualTo(LocalTime.of(13, 0));
            assertThat(stored.getNote()).isEqualTo("Ghi chú mới");
            assertThat(stored.getCostAmount()).isEqualByComparingTo("12.50");
            assertThat(stored.getCurrency()).isEqualTo("USD");
            assertThat(stored.getBookingUrl()).isEqualTo("https://example.com/new");
        }

        @Test
        void blankNoteAndBookingUrlAreCleared() {
            update(new UpdateActivityRequest(null, null, null, null, "   ", null, null, ""));

            assertThat(stored.getNote()).isNull();
            assertThat(stored.getBookingUrl()).isNull();
            // The rest is untouched
            assertThat(stored.getTitle()).isEqualTo("Ăn sáng");
            assertThat(stored.getCostAmount()).isEqualByComparingTo("50000");
        }

        @Test
        void secondsSentByTheClientAreDropped() {
            update(times(LocalTime.of(9, 30, 40), LocalTime.of(10, 45, 59)));

            assertThat(stored.getStartTime()).isEqualTo(LocalTime.of(9, 30));
            assertThat(stored.getEndTime()).isEqualTo(LocalTime.of(10, 45));
        }

        // ---- time rules on the merged state ------------------------------------------------------------------

        @Test
        void endTimeAloneIsCheckedAgainstTheStoredStartTime() {
            assertThatThrownBy(() -> update(times(null, LocalTime.of(8, 0))))
                    .satisfies(ex -> assertSingleViolation(ex, "endTime", "error.activity.end-not-after-start"));

            assertThat(stored.getEndTime()).isEqualTo(TEN);
            verify(activityRepository, never()).saveAndFlush(any());
        }

        @Test
        void startTimeAloneIsCheckedAgainstTheStoredEndTime() {
            assertThatThrownBy(() -> update(times(TEN_THIRTY, null)))
                    .satisfies(ex -> assertSingleViolation(ex, "endTime", "error.activity.end-not-after-start"));

            assertThat(stored.getStartTime()).isEqualTo(NINE);
            verify(activityRepository, never()).saveAndFlush(any());
        }

        @Test
        void endTimeOnAnActivityWithoutStartTimeIsRejectedOnStartTime() {
            stored.setStartTime(null);
            stored.setEndTime(null);

            assertThatThrownBy(() -> update(times(null, TEN)))
                    .satisfies(ex -> assertSingleViolation(ex, "startTime", "error.activity.start-time-required"));
            assertThat(stored.getEndTime()).isNull();
        }

        @Test
        void timesCanBeAddedToAnUnscheduledActivity() {
            stored.setStartTime(null);
            stored.setEndTime(null);

            update(times(NINE, TEN));

            assertThat(stored.getStartTime()).isEqualTo(NINE);
            assertThat(stored.getEndTime()).isEqualTo(TEN);
        }

        // ---- rule 14.4 -----------------------------------------------------------------------------------------

        @Test
        void movingIntoAnotherActivityIsRejectedWith409AndNothingChanges() {
            when(activityRepository.findTimedByTripDayId(DAY_ID))
                    .thenReturn(List.of(stored, existing(32L, "Cà phê", "10:00", "11:00")));

            assertThatThrownBy(() -> update(times(LocalTime.of(9, 30), TEN_THIRTY)))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_TIME_CONFLICT);
                        assertThat(ex.getDetails()).containsExactly(FieldViolation.of("startTime",
                                "error.activity.time-conflict-with", "Cà phê", "10:00", "11:00"));
                    });

            assertThat(stored.getStartTime()).isEqualTo(NINE);
            assertThat(stored.getEndTime()).isEqualTo(TEN);
            verify(activityRepository, never()).saveAndFlush(any());
        }

        @Test
        void anActivityIsNeverItsOwnConflict() {
            // The candidates contain the activity itself with its old range 09:00–10:00
            when(activityRepository.findTimedByTripDayId(DAY_ID)).thenReturn(List.of(stored));

            update(times(null, TEN_THIRTY));

            assertThat(stored.getEndTime()).isEqualTo(TEN_THIRTY);
        }

        @Test
        void unchangedRangeIsNotCheckedSoAnAllowedOverlapCanStillBeRenamed() {
            update(titled("Ăn sáng muộn"));
            // Sending the stored times again is not a change either
            update(times(NINE, TEN));

            verify(activityRepository, never()).findTimedByTripDayId(anyLong());
        }

        @Test
        void allowOverlapSkipsTheCheckAndSaves() {
            activityService.update(TRIP_ID, ACTIVITY_ID, times(LocalTime.of(9, 30), TEN_THIRTY), true);

            assertThat(stored.getStartTime()).isEqualTo(LocalTime.of(9, 30));
            verify(activityRepository, never()).findTimedByTripDayId(anyLong());
        }

        // ---- cost and currency -----------------------------------------------------------------------------------

        @Test
        void costOnAnActivityWithoutCurrencyTakesTheCurrencyOfTheTrip() {
            stored.setCostAmount(null);
            stored.setCurrency(null);

            update(new UpdateActivityRequest(null, null, null, null, null, new BigDecimal("12.50"), null, null));

            assertThat(stored.getCostAmount()).isEqualByComparingTo("12.50");
            assertThat(stored.getCurrency()).isEqualTo("USD");
        }

        @Test
        void newCostKeepsTheStoredCurrency() {
            update(new UpdateActivityRequest(null, null, null, null, null, new BigDecimal("75000"), null, null));

            assertThat(stored.getCostAmount()).isEqualByComparingTo("75000");
            assertThat(stored.getCurrency()).isEqualTo("VND");
        }

        // ---- not found ---------------------------------------------------------------------------------------------

        @Test
        void missingOrDeletedTripIsNotFoundAndTheActivityIsNotLoaded() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> update(titled("X")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Trip");
            verifyNoInteractions(activityRepository);
        }

        @Test
        void activityOfAnotherTripIsNotFoundAndNothingIsSaved() {
            when(activityRepository.findByIdAndTripId(ACTIVITY_ID, TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> update(titled("X")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Activity");
            verify(activityRepository, never()).saveAndFlush(any());
            assertThat(stored.getTitle()).isEqualTo("Ăn sáng");
        }

        /** The default case of the endpoint: allowOverlap = false. */
        private ActivityResponse update(UpdateActivityRequest request) {
            return activityService.update(TRIP_ID, ACTIVITY_ID, request, false);
        }

        private static UpdateActivityRequest titled(String title) {
            return new UpdateActivityRequest(title, null, null, null, null, null, null, null);
        }

        private static UpdateActivityRequest times(LocalTime start, LocalTime end) {
            return new UpdateActivityRequest(null, null, start, end, null, null, null, null);
        }

    }

    @Nested
    class Reorder {

        private static final long DAY_TWO_ID = 12L;

        private TripDay dayTwo;
        private Activity breakfast;
        private Activity sightseeing;
        private Activity market;

        /** What the database holds; tests about times add activities that have a range. */
        private final List<Activity> stored = new ArrayList<>();

        @BeforeEach
        void twoDaysWithThreeActivities() {
            dayTwo = TripDay.builder().trip(trip).dayIndex(2).date(OCT_1.plusDays(1)).build();
            ReflectionTestUtils.setField(dayTwo, "id", DAY_TWO_ID);
            // Day 1: breakfast 1000, sightseeing 2000. Day 2: market 1000
            breakfast = placed(31L, day, "Ăn sáng", 1000);
            sightseeing = placed(32L, day, "Tham quan", 2000);
            market = placed(41L, dayTwo, "Chợ đêm", 1000);
            stored.addAll(List.of(breakfast, sightseeing, market));

            // lenient: the rejected requests stop before some of these are reached
            lenient().when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            lenient().when(tripDayRepository.findAllByIdInAndTripId(anyCollection(), eq(TRIP_ID)))
                    .thenAnswer(invocation -> {
                        Collection<Long> ids = invocation.getArgument(0);
                        return Stream.of(day, dayTwo).filter(d -> ids.contains(d.getId())).toList();
                    });
            lenient().when(activityRepository.findAllByIdInAndTripId(anyCollection(), eq(TRIP_ID)))
                    .thenAnswer(invocation -> {
                        Collection<Long> ids = invocation.getArgument(0);
                        return stored.stream().filter(a -> ids.contains(a.getId())).toList();
                    });
            lenient().when(activityRepository.findTimedByTripDayIdIn(anyCollection()))
                    .thenAnswer(invocation -> {
                        Collection<Long> dayIds = invocation.getArgument(0);
                        return stored.stream()
                                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                                .filter(a -> dayIds.contains(a.getTripDay().getId()))
                                .sorted(Comparator.comparing(Activity::getStartTime).thenComparing(Activity::getId))
                                .toList();
                    });
            // What the database would answer after the flush: the current state, in display order
            lenient().when(activityRepository.findByTripDayIdInDisplayOrder(anyCollection()))
                    .thenAnswer(invocation -> {
                        Collection<Long> dayIds = invocation.getArgument(0);
                        return stored.stream()
                                .filter(a -> dayIds.contains(a.getTripDay().getId()))
                                .sorted(Comparator.comparingInt(Activity::getOrderIndex).thenComparing(Activity::getId))
                                .toList();
                    });
        }

        @Test
        void movesAnActivityInsideItsDayAndReturnsThatDayInTheNewOrder() {
            // sightseeing is dragged above breakfast
            List<TripDayDetailResponse> days = reorder(moves(move(32L, DAY_ID, 500)));

            assertThat(sightseeing.getOrderIndex()).isEqualTo(500);
            assertThat(sightseeing.getTripDay()).isSameAs(day);
            assertThat(breakfast.getOrderIndex()).isEqualTo(1000);
            verify(activityRepository).flush();

            assertThat(days).extracting(TripDayDetailResponse::id).containsExactly(DAY_ID);
            assertThat(days.getFirst().activities())
                    .extracting(ActivityResponse::title, ActivityResponse::orderIndex)
                    .containsExactly(tuple("Tham quan", 500), tuple("Ăn sáng", 1000));
        }

        @Test
        void movesAnActivityToAnotherDayAndReturnsBothDaysInCalendarOrder() {
            // market is dragged from day 2 to day 1, between breakfast and sightseeing
            List<TripDayDetailResponse> days = reorder(moves(move(41L, DAY_ID, 1500)));

            assertThat(market.getTripDay()).isSameAs(day);
            assertThat(market.getOrderIndex()).isEqualTo(1500);

            assertThat(days).extracting(TripDayDetailResponse::id).containsExactly(DAY_ID, DAY_TWO_ID);
            assertThat(days.get(0).activities())
                    .extracting(ActivityResponse::title, ActivityResponse::dayId)
                    .containsExactly(tuple("Ăn sáng", DAY_ID), tuple("Chợ đêm", DAY_ID), tuple("Tham quan", DAY_ID));
            // The day that lost its only activity comes back too, empty, so the client can redraw it
            assertThat(days.get(1).activities()).isEmpty();
        }

        @Test
        void appliesSeveralMovesOfOneRequestTogether() {
            reorder(moves(
                    move(31L, DAY_TWO_ID, 2000),
                    move(41L, DAY_ID, 3000),
                    move(32L, DAY_ID, 1000)));

            assertThat(breakfast.getTripDay()).isSameAs(dayTwo);
            assertThat(breakfast.getOrderIndex()).isEqualTo(2000);
            assertThat(market.getTripDay()).isSameAs(day);
            assertThat(market.getOrderIndex()).isEqualTo(3000);
            assertThat(sightseeing.getOrderIndex()).isEqualTo(1000);
        }

        @Test
        void keepsTheContentOfTheActivity() {
            reorder(moves(move(31L, DAY_TWO_ID, 2000)));

            assertThat(breakfast.getTitle()).isEqualTo("Ăn sáng");
            assertThat(breakfast.getCreatedBy()).isSameAs(creator);
        }

        @Test
        void sameActivityTwiceIsRejectedAndNothingMoves() {
            assertThatThrownBy(() -> reorder(moves(
                    move(31L, DAY_ID, 500),
                    move(32L, DAY_ID, 700),
                    move(31L, DAY_TWO_ID, 900))))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                        assertThat(ex.getDetails()).containsExactly(
                                FieldViolation.of("items", "error.reorder.duplicate-activity", "31"));
                    });

            assertNothingMoved();
        }

        @Test
        void activityOfAnotherTripRejectsTheWholeBatchAndNothingMoves() {
            // 99 is not an activity of this trip; the valid move before it must not be applied either
            assertThatThrownBy(() -> reorder(moves(
                    move(32L, DAY_ID, 500),
                    move(99L, DAY_ID, 700))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Activity")
                    .hasMessageContaining("99");

            assertNothingMoved();
        }

        @Test
        void dayOfAnotherTripRejectsTheWholeBatchAndNothingMoves() {
            assertThatThrownBy(() -> reorder(moves(
                    move(32L, DAY_ID, 500),
                    move(31L, 77L, 1000))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("TripDay")
                    .hasMessageContaining("77");

            assertNothingMoved();
        }

        @Test
        void missingOrDeletedTripIsNotFoundAndNothingIsLoaded() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(false);

            assertThatThrownBy(() -> reorder(moves(move(31L, DAY_ID, 500))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Trip");
            verifyNoInteractions(activityRepository, tripDayRepository);
        }

        // ---- rule 14.4: an activity that changes day takes its times with it --------------------------------

        @Test
        void movingIntoADayWhereItOverlapsIsRejectedWith409AndNothingMoves() {
            Activity lunch = timedAt(51L, day, "Ăn trưa", "11:30", "13:00");
            Activity coffee = timedAt(52L, dayTwo, "Cà phê", "12:00", "12:30");

            assertThatThrownBy(() -> reorder(moves(
                    move(32L, DAY_ID, 500),
                    move(52L, DAY_ID, 1500))))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_TIME_CONFLICT);
                        // The second item of the request is the one in conflict
                        assertThat(ex.getDetails()).containsExactly(FieldViolation.of("items[1].dayId",
                                "error.reorder.time-conflict", "Cà phê", "Ăn trưa", "11:30", "13:00"));
                    });

            assertThat(coffee.getTripDay()).isSameAs(dayTwo);
            assertThat(lunch.getTripDay()).isSameAs(day);
            assertNothingMoved();
        }

        @Test
        void movingNextToAnActivityItOnlyTouchesIsAllowed() {
            timedAt(51L, day, "Ăn trưa", "11:30", "13:00");
            Activity coffee = timedAt(52L, dayTwo, "Cà phê", "13:00", "13:30");

            reorder(moves(move(52L, DAY_ID, 3000)));

            assertThat(coffee.getTripDay()).isSameAs(day);
        }

        @Test
        void allowOverlapSkipsTheCheckAndMoves() {
            timedAt(51L, day, "Ăn trưa", "11:30", "13:00");
            Activity coffee = timedAt(52L, dayTwo, "Cà phê", "12:00", "12:30");

            activityService.reorder(TRIP_ID, moves(move(52L, DAY_ID, 1500)), true);

            assertThat(coffee.getTripDay()).isSameAs(day);
            verify(activityRepository, never()).findTimedByTripDayIdIn(anyCollection());
        }

        @Test
        void reorderInsideADayIsNeverCheckedEvenWhenItsActivitiesOverlap() {
            // Saved earlier with allowOverlap: they overlap, and may still be reordered
            Activity lunch = timedAt(51L, day, "Ăn trưa", "11:30", "13:00");
            Activity coffee = timedAt(52L, day, "Cà phê", "12:00", "12:30");

            reorder(moves(move(52L, DAY_ID, 500), move(51L, DAY_ID, 700)));

            assertThat(coffee.getOrderIndex()).isEqualTo(500);
            assertThat(lunch.getOrderIndex()).isEqualTo(700);
            verify(activityRepository, never()).findTimedByTripDayIdIn(anyCollection());
        }

        @Test
        void activityWithoutAFullRangeIsNeverChecked() {
            timedAt(51L, day, "Ăn trưa", "11:30", "13:00");
            Activity startOnly = placed(53L, dayTwo, "Chỉ có giờ bắt đầu", 2000);
            startOnly.setStartTime(LocalTime.of(12, 0));
            stored.add(startOnly);

            reorder(moves(move(53L, DAY_ID, 1500), move(41L, DAY_ID, 1700)));

            assertThat(startOnly.getTripDay()).isSameAs(day);
            verify(activityRepository, never()).findTimedByTripDayIdIn(anyCollection());
        }

        @Test
        void twoActivitiesArrivingTogetherAreComparedWithEachOtherAndBothReported() {
            // Day 1 has nothing at noon; the two arrivals overlap each other
            timedAt(52L, dayTwo, "Cà phê", "12:00", "12:30");
            TripDay dayThree = TripDay.builder().trip(trip).dayIndex(3).date(OCT_1.plusDays(2)).build();
            ReflectionTestUtils.setField(dayThree, "id", 13L);
            timedAt(61L, dayThree, "Ăn trưa", "11:30", "13:00");
            when(tripDayRepository.findAllByIdInAndTripId(anyCollection(), eq(TRIP_ID)))
                    .thenReturn(List.of(day, dayTwo, dayThree));

            assertThatThrownBy(() -> reorder(moves(
                    move(52L, DAY_ID, 1500),
                    move(61L, DAY_ID, 1700))))
                    .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_TIME_CONFLICT);
                        assertThat(ex.getDetails()).containsExactly(
                                FieldViolation.of("items[0].dayId", "error.reorder.time-conflict",
                                        "Cà phê", "Ăn trưa", "11:30", "13:00"),
                                FieldViolation.of("items[1].dayId", "error.reorder.time-conflict",
                                        "Ăn trưa", "Cà phê", "12:00", "12:30"));
                    });
            assertNothingMoved();
        }

        @Test
        void twoOverlappingActivitiesCanSwapDaysInOneRequest() {
            // Each one overlaps what the other day holds now, but not what it will hold after the swap
            Activity lunch = timedAt(51L, day, "Ăn trưa", "11:30", "13:00");
            Activity coffee = timedAt(52L, dayTwo, "Cà phê", "12:00", "12:30");

            reorder(moves(move(51L, DAY_TWO_ID, 2000), move(52L, DAY_ID, 3000)));

            assertThat(lunch.getTripDay()).isSameAs(dayTwo);
            assertThat(coffee.getTripDay()).isSameAs(day);
        }

        @Test
        void anActivityThatStaysInItsDayStillBlocksAnArrival() {
            // lunch is part of the request too, but it only changes position inside day 1
            timedAt(51L, day, "Ăn trưa", "11:30", "13:00");
            timedAt(52L, dayTwo, "Cà phê", "12:00", "12:30");

            assertThatThrownBy(() -> reorder(moves(
                    move(51L, DAY_ID, 500),
                    move(52L, DAY_ID, 1500))))
                    .isInstanceOfSatisfying(BusinessRuleException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTIVITY_TIME_CONFLICT));
            assertNothingMoved();
        }

        /** The default case of the endpoint: allowOverlap = false. */
        private List<TripDayDetailResponse> reorder(ReorderActivitiesRequest request) {
            return activityService.reorder(TRIP_ID, request, false);
        }

        private Activity timedAt(long id, TripDay onDay, String title, String start, String end) {
            Activity activity = placed(id, onDay, title, 5000);
            activity.setStartTime(LocalTime.parse(start));
            activity.setEndTime(LocalTime.parse(end));
            stored.add(activity);
            return activity;
        }

        private void assertNothingMoved() {
            assertThat(breakfast.getTripDay()).isSameAs(day);
            assertThat(breakfast.getOrderIndex()).isEqualTo(1000);
            assertThat(sightseeing.getTripDay()).isSameAs(day);
            assertThat(sightseeing.getOrderIndex()).isEqualTo(2000);
            assertThat(market.getTripDay()).isSameAs(dayTwo);
            assertThat(market.getOrderIndex()).isEqualTo(1000);
            verify(activityRepository, never()).flush();
        }

        private Activity placed(long id, TripDay onDay, String title, int orderIndex) {
            Activity activity = Activity.builder().tripDay(onDay).title(title).orderIndex(orderIndex)
                    .createdBy(creator).build();
            ReflectionTestUtils.setField(activity, "id", id);
            return activity;
        }

        private static ReorderActivitiesRequest moves(ReorderActivitiesRequest.Item... items) {
            return new ReorderActivitiesRequest(List.of(items));
        }

        private static ReorderActivitiesRequest.Item move(long activityId, long dayId, int orderIndex) {
            return new ReorderActivitiesRequest.Item(activityId, dayId, orderIndex);
        }

    }

    @Nested
    class Delete {

        private static final long ACTIVITY_ID = 31L;

        @Test
        void deletesTheActivityOfTheTrip() {
            Activity stored = existing(ACTIVITY_ID, "Ăn sáng", "09:00", "10:00");
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(activityRepository.findByIdAndTripId(ACTIVITY_ID, TRIP_ID)).thenReturn(Optional.of(stored));

            activityService.delete(TRIP_ID, ACTIVITY_ID);

            verify(activityRepository).delete(stored);
        }

        @Test
        void missingOrDeletedTripIsNotFoundAndTheActivityIsNotLoaded() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(false);

            assertThatThrownBy(() -> activityService.delete(TRIP_ID, ACTIVITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Trip");
            verifyNoInteractions(activityRepository);
        }

        @Test
        void activityOfAnotherTripIsNotFoundAndNothingIsDeleted() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(activityRepository.findByIdAndTripId(ACTIVITY_ID, TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> activityService.delete(TRIP_ID, ACTIVITY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Activity");
            verify(activityRepository, never()).delete(any(Activity.class));
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
