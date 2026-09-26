package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.PageResponse;
import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.common.util.SlugGenerator;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.request.CreateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.exception.SlugGenerationException;
import com.trieu.tripplanner.mapper.TripMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit test: repositories and the slug generator are mocked, the MapStruct mapper is real.
 * Permission is not tested here on purpose: it lives in @PreAuthorize (TripControllerTest,
 * TripFlowIntegrationTest), never in the service (CLAUDE.md rule 15).
 */
@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final long USER_ID = 7L;
    private static final long TRIP_ID = 5L;
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SlugGenerator slugGenerator;

    private final TripMapper tripMapper = Mappers.getMapper(TripMapper.class);

    private TripServiceImpl tripService;

    @BeforeEach
    void setUp() {
        tripService = new TripServiceImpl(tripRepository, userRepository, tripMapper, slugGenerator);
    }

    @Nested
    class Create {

        @BeforeEach
        void ownerAndFreeSlug() {
            // lenient: validation tests throw before the owner or a slug is needed
            lenient().when(userRepository.getReferenceById(USER_ID))
                    .thenReturn(TestUsers.verified(USER_ID, "owner@example.com"));
            lenient().when(slugGenerator.generate(anyString())).thenReturn("da-lat-abc123");
            lenient().when(tripRepository.save(any(Trip.class)))
                    .thenAnswer(invocation -> withId(invocation.getArgument(0), TRIP_ID));
        }

        @Test
        void createsDraftTripWithDefaultsForOwnerFromToken() {
            TripResponse response = tripService.create(USER_ID,
                    createRequest("  Đà Lạt 3 ngày  ", OCT_1, OCT_1.plusDays(2), null, null));

            Trip saved = savedTrip();
            assertThat(saved.getOwner().getId()).isEqualTo(USER_ID);
            assertThat(saved.getTitle()).isEqualTo("Đà Lạt 3 ngày");
            assertThat(saved.getSlug()).isEqualTo("da-lat-abc123");
            assertThat(saved.getStatus()).isEqualTo(TripStatus.DRAFT);
            assertThat(saved.getVisibility()).isEqualTo(TripVisibility.PRIVATE);
            assertThat(saved.getCurrency()).isEqualTo("VND");
            assertThat(response.id()).isEqualTo(TRIP_ID);
            assertThat(response.ownerId()).isEqualTo(USER_ID);
        }

        @Test
        void keepsCurrencyAndVisibilityGivenByClient() {
            CreateTripRequest request = new CreateTripRequest("Tokyo", null, null, null, null, null,
                    OCT_1, OCT_1, new BigDecimal("1500.50"), "USD", TripVisibility.LINK);

            tripService.create(USER_ID, request);

            Trip saved = savedTrip();
            assertThat(saved.getCurrency()).isEqualTo("USD");
            assertThat(saved.getVisibility()).isEqualTo(TripVisibility.LINK);
            assertThat(saved.getBudgetAmount()).isEqualByComparingTo("1500.50");
        }

        @Test
        void retriesSlugWhenCandidateIsTaken() {
            when(slugGenerator.generate(anyString())).thenReturn("hue-111111", "hue-222222");
            when(tripRepository.countBySlugIncludingDeleted("hue-111111")).thenReturn(1L);
            when(tripRepository.countBySlugIncludingDeleted("hue-222222")).thenReturn(0L);

            tripService.create(USER_ID, createRequest("Huế", OCT_1, OCT_1, null, null));

            assertThat(savedTrip().getSlug()).isEqualTo("hue-222222");
        }

        @Test
        void givesUpAfterFiveTakenSlugs() {
            when(tripRepository.countBySlugIncludingDeleted(anyString())).thenReturn(1L);

            assertThatThrownBy(() -> tripService.create(USER_ID, createRequest("Huế", OCT_1, OCT_1, null, null)))
                    .isInstanceOf(SlugGenerationException.class);
            verify(slugGenerator, times(TripServiceImpl.SLUG_ATTEMPTS)).generate(anyString());
            verify(tripRepository, never()).save(any());
        }

        @Test
        void acceptsExactlySixtyDaysIncludingBothEnds() {
            tripService.create(USER_ID, createRequest("Xuyên Việt", OCT_1, OCT_1.plusDays(59), null, null));

            verify(tripRepository).save(any(Trip.class));
        }

        @Test
        void rejectsSixtyOneDaysOnEndDate() {
            assertThatThrownBy(() -> tripService.create(USER_ID,
                    createRequest("Xuyên Việt", OCT_1, OCT_1.plusDays(60), null, null)))
                    .satisfies(ex -> assertSingleViolation(ex, "endDate", "error.trip.too-long", 60));
            verify(tripRepository, never()).save(any());
        }

        @Test
        void rejectsEndBeforeStartOnEndDate() {
            assertThatThrownBy(() -> tripService.create(USER_ID,
                    createRequest("Ngược", OCT_1, OCT_1.minusDays(1), null, null)))
                    .satisfies(ex -> assertSingleViolation(ex, "endDate", "error.trip.end-before-start"));
            verify(tripRepository, never()).save(any());
        }

        @Test
        void rejectsLatitudeWithoutLongitudeOnTheMissingField() {
            assertThatThrownBy(() -> tripService.create(USER_ID,
                    createRequest("Nửa toạ độ", OCT_1, OCT_1, new BigDecimal("11.94"), null)))
                    .satisfies(ex -> assertSingleViolation(ex, "destinationLng", "error.trip.coordinates-incomplete"));
        }

        private Trip savedTrip() {
            ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
            verify(tripRepository).save(captor.capture());
            return captor.getValue();
        }

    }

    @Nested
    class Update {

        private Trip stored;

        @BeforeEach
        void storedTrip() {
            stored = withId(Trip.builder()
                    .owner(TestUsers.verified(USER_ID, "owner@example.com"))
                    .title("Đà Lạt")
                    .slug("da-lat-abc123")
                    .description("Mô tả cũ")
                    .startDate(OCT_1)
                    .endDate(OCT_1.plusDays(2))
                    .status(TripStatus.PLANNED)
                    .build(), TRIP_ID);
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(stored));
            lenient().when(tripRepository.saveAndFlush(stored)).thenReturn(stored);
        }

        @Test
        void changesOnlyTheFieldsSentAndKeepsSlugAndStatus() {
            TripResponse response = tripService.update(TRIP_ID,
                    updateRequest("  Đà Lạt mùa hoa ", null, null, null, TripVisibility.PUBLIC));

            assertThat(stored.getTitle()).isEqualTo("Đà Lạt mùa hoa");
            assertThat(stored.getVisibility()).isEqualTo(TripVisibility.PUBLIC);
            assertThat(stored.getDescription()).isEqualTo("Mô tả cũ");
            assertThat(stored.getEndDate()).isEqualTo(OCT_1.plusDays(2));
            assertThat(stored.getSlug()).isEqualTo("da-lat-abc123");
            assertThat(stored.getStatus()).isEqualTo(TripStatus.PLANNED);
            assertThat(response.title()).isEqualTo("Đà Lạt mùa hoa");
            verify(tripRepository).saveAndFlush(stored);
        }

        @Test
        void validatesEndDateAgainstStoredStartDate() {
            assertThatThrownBy(() -> tripService.update(TRIP_ID,
                    updateRequest(null, null, OCT_1.minusDays(1), null, null)))
                    .satisfies(ex -> assertSingleViolation(ex, "endDate", "error.trip.end-before-start"));
            verify(tripRepository, never()).saveAndFlush(any());
        }

        @Test
        void validatesNewStartDateAgainstStoredEndDate() {
            // Moving start 70 days earlier makes the stored range 73 days long
            assertThatThrownBy(() -> tripService.update(TRIP_ID,
                    updateRequest(null, OCT_1.minusDays(70), null, null, null)))
                    .satisfies(ex -> assertSingleViolation(ex, "endDate", "error.trip.too-long", 60));
        }

        @Test
        void validatesCoordinatesOnMergedState() {
            assertThatThrownBy(() -> tripService.update(TRIP_ID,
                    updateRequest(null, null, null, new BigDecimal("16.46"), null)))
                    .satisfies(ex -> assertSingleViolation(ex, "destinationLng", "error.trip.coordinates-incomplete"));
        }

    }

    @Nested
    class ReadAndDelete {

        @Test
        void getMissingTripThrowsNotFound() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.get(TRIP_ID)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void deleteHandsTheEntityToTheRepositoryForSoftDelete() {
            Trip trip = withId(minimalTrip(), TRIP_ID);
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));

            tripService.delete(TRIP_ID);

            // repository.delete runs Trip's @SQLDelete (UPDATE deleted_at), proven in TripMappingTest
            verify(tripRepository).delete(trip);
        }

        @Test
        void deleteMissingTripThrowsNotFoundAndDeletesNothing() {
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripService.delete(TRIP_ID)).isInstanceOf(ResourceNotFoundException.class);
            verify(tripRepository, never()).delete(any(Trip.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        void listMapsThePageFromTheRepository() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            when(tripRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(withId(minimalTrip(), TRIP_ID)), pageable, 21));

            PageResponse<TripSummaryResponse> page = tripService.list(USER_ID,
                    new TripFilter(null, null, null, null), pageable);

            assertThat(page.items()).extracting(TripSummaryResponse::id).containsExactly(TRIP_ID);
            assertThat(page.totalElements()).isEqualTo(21);
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        void listRejectsUnsupportedSortWithoutQuerying() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by("owner.passwordHash"));

            assertThatThrownBy(() -> tripService.list(USER_ID, new TripFilter(null, null, null, null), pageable))
                    .satisfies(ex -> assertSingleViolation(ex, "sort", "error.trip.sort-unsupported",
                            String.join(", ", TripServiceImpl.SORTABLE_PROPERTIES)));
            verifyNoInteractions(tripRepository);
        }

    }

    private static void assertSingleViolation(Throwable ex, String field, String messageKey, Object... args) {
        assertThat(ex).isInstanceOf(BusinessRuleException.class);
        BusinessRuleException rule = (BusinessRuleException) ex;
        assertThat(rule.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(rule.getDetails()).containsExactly(FieldViolation.of(field, messageKey, args));
    }

    private static CreateTripRequest createRequest(String title, LocalDate start, LocalDate end,
                                                   BigDecimal lat, BigDecimal lng) {
        return new CreateTripRequest(title, null, null, null, lat, lng, start, end, null, null, null);
    }

    private static UpdateTripRequest updateRequest(String title, LocalDate start, LocalDate end,
                                                   BigDecimal lat, TripVisibility visibility) {
        return new UpdateTripRequest(title, null, null, null, lat, null, start, end, null, null, visibility);
    }

    private static Trip minimalTrip() {
        User owner = TestUsers.verified(USER_ID, "owner@example.com");
        return Trip.builder().owner(owner).title("Huế").slug("hue-abc123").startDate(OCT_1).endDate(OCT_1).build();
    }

    /** BaseEntity has no id setter on purpose; tests set it the way Hibernate would. */
    private static Trip withId(Trip trip, long id) {
        ReflectionTestUtils.setField(trip, "id", id);
        ReflectionTestUtils.setField(trip, "version", 0L);
        return trip;
    }

}
