package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.dto.request.CreateManualPlaceRequest;
import com.trieu.tripplanner.dto.request.SavePlaceRequest;
import com.trieu.tripplanner.dto.response.PlaceResponse;
import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.PlaceMapper;
import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.repository.PlaceRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The map source is a mock; the MapStruct mapper is the real generated one.
 */
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    private static final PlaceResult LINH_UNG = new PlaceResult(PlaceProvider.MOCK, "da-nang-chua-linh-ung",
            "Chùa Linh Ứng", "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"),
            new BigDecimal("108.2784112"), "SIGHTSEEING");

    @Mock
    private MapProvider mapProvider;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(mapProvider, placeRepository, userRepository, Mappers.getMapper(PlaceMapper.class));
        // The source in use; lenient because search and createManual never ask for it
        lenient().when(mapProvider.provider()).thenReturn(PlaceProvider.MOCK);
    }

    @Test
    void searchTrimsTheKeywordAndReturnsEveryFieldOfTheResultsInTheSourceOrder() {
        when(mapProvider.search("linh ung", 8, null)).thenReturn(List.of(
                new PlaceResult(PlaceProvider.MOCK, "da-nang-chua-linh-ung", "Chùa Linh Ứng",
                        "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"),
                        new BigDecimal("108.2784112"), "SIGHTSEEING"),
                new PlaceResult(PlaceProvider.MOCK, "x-2", "Linh Ứng Bãi Bụt", null, new BigDecimal("16.1"),
                        new BigDecimal("108.2"), null)));

        List<PlaceResultResponse> results = placeService.search("  linh ung ", 8, null, null);

        assertThat(results).containsExactly(
                new PlaceResultResponse(PlaceProvider.MOCK, "da-nang-chua-linh-ung", "Chùa Linh Ứng",
                        "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"),
                        new BigDecimal("108.2784112"), "SIGHTSEEING"),
                new PlaceResultResponse(PlaceProvider.MOCK, "x-2", "Linh Ứng Bãi Bụt", null, new BigDecimal("16.1"),
                        new BigDecimal("108.2"), null));
    }

    @Test
    void searchWithNoMatchReturnsAnEmptyList() {
        when(mapProvider.search("khong co", 8, null)).thenReturn(List.of());

        assertThat(placeService.search("khong co", 8, null, null)).isEmpty();
    }

    @Test
    void searchPassesTheReferencePointWhenBothNumbersAreSent() {
        BigDecimal lat = new BigDecimal("16.0544");
        BigDecimal lng = new BigDecimal("108.2022");
        when(mapProvider.search("cho", 8, new Coordinate(lat, lng))).thenReturn(List.of());

        placeService.search("cho", 8, lat, lng);

        verify(mapProvider).search("cho", 8, new Coordinate(lat, lng));
    }

    @Test
    void halfACoordinateIsAValidationErrorOnTheMissingNumber() {
        assertThatThrownBy(() -> placeService.search("cho", 8, new BigDecimal("16.05"), null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException rule = (BusinessRuleException) ex;
                    assertThat(rule.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(rule.getDetails()).extracting(FieldViolation::field).containsExactly("lng");
                });
        assertThatThrownBy(() -> placeService.search("cho", 8, null, new BigDecimal("108.2")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .extracting(FieldViolation::field).containsExactly("lat"));

        verifyNoInteractions(mapProvider);
    }

    // ---------- getOrCreate ----------

    @Test
    void firstPickStoresACopyBuiltFromTheSourceNotFromTheRequest() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.empty());
        when(mapProvider.lookup("da-nang-chua-linh-ung")).thenReturn(Optional.of(LINH_UNG));
        when(placeRepository.saveAndFlush(any(Place.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 31L));

        PlaceResponse response = placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung"));

        ArgumentCaptor<Place> stored = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).saveAndFlush(stored.capture());
        // The request carries two values only; every fact of the place comes from the lookup
        assertThat(stored.getValue().getProvider()).isEqualTo(PlaceProvider.MOCK);
        assertThat(stored.getValue().getExternalId()).isEqualTo("da-nang-chua-linh-ung");
        assertThat(stored.getValue().getName()).isEqualTo("Chùa Linh Ứng");
        assertThat(stored.getValue().getAddress()).isEqualTo("Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng");
        assertThat(stored.getValue().getLat()).isEqualByComparingTo("16.1001567");
        assertThat(stored.getValue().getLng()).isEqualByComparingTo("108.2784112");
        assertThat(stored.getValue().getCategory()).isEqualTo("SIGHTSEEING");
        assertThat(response).isEqualTo(new PlaceResponse(31L, PlaceProvider.MOCK, "Chùa Linh Ứng",
                "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"), new BigDecimal("108.2784112"),
                "SIGHTSEEING"));
    }

    @Test
    void secondPickReturnsTheStoredCopyWithoutAskingTheSourceOrWritingAgain() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.of(storedLinhUng(31L)));

        PlaceResponse response = placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung"));

        assertThat(response.id()).isEqualTo(31L);
        verify(placeRepository, never()).saveAndFlush(any());
        verify(mapProvider, never()).lookup(any());
    }

    @Test
    void externalIdIsTrimmedBeforeItIsLookedUp() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.of(storedLinhUng(31L)));

        assertThat(placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "  da-nang-chua-linh-ung ")).id())
                .isEqualTo(31L);
    }

    @Test
    void placeTheSourceDoesNotKnowIsNotFoundAndNothingIsStored() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "bia-ra")).thenReturn(Optional.empty());
        when(mapProvider.lookup("bia-ra")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "bia-ra")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(placeRepository, never()).saveAndFlush(any());
    }

    @Test
    void whenAnotherRequestStoresTheSamePlaceFirstItsRowIsReturned() {
        // Both requests saw "not stored yet"; the UNIQUE key lets only one INSERT through
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(storedLinhUng(31L)));
        when(mapProvider.lookup("da-nang-chua-linh-ung")).thenReturn(Optional.of(LINH_UNG));
        when(placeRepository.saveAndFlush(any(Place.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_places_provider_external_id'"));

        PlaceResponse response = placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung"));

        assertThat(response.id()).isEqualTo(31L);
    }

    @Test
    void integrityErrorThatIsNotADuplicateIsNotSwallowed() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Column 'name' cannot be null");
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.empty());
        when(mapProvider.lookup("da-nang-chua-linh-ung")).thenReturn(Optional.of(LINH_UNG));
        when(placeRepository.saveAndFlush(any(Place.class))).thenThrow(failure);

        // Nothing to read back: the original error must reach the caller, not a made-up "not found"
        assertThatThrownBy(() -> placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung")))
                .isSameAs(failure);
    }

    @Test
    void pickFromASourceThatIsNotInUseIsRejectedBeforeAnythingIsReadOrStored() {
        // MANUAL is a provider of the enum but never a map source: a place typed by hand has its own endpoint
        assertThatThrownBy(() -> placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MANUAL, "da-nang-chua-linh-ung")))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(ex.getDetails()).extracting(FieldViolation::field).containsExactly("provider");
                    assertThat(ex.getDetails()).extracting(FieldViolation::messageKey)
                            .containsExactly("error.place.provider-not-active");
                });
        verifyNoInteractions(placeRepository);
        verify(mapProvider, never()).lookup(any());
    }

    // ---------- createManual ----------

    @Test
    void manualPlaceIsStoredAsTypedAndBelongsToTheSignedInUser() {
        User creator = TestUsers.verified(7L, "an@example.com");
        when(userRepository.getReferenceById(7L)).thenReturn(creator);
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 40L));

        PlaceResponse response = placeService.createManual(7L, new CreateManualPlaceRequest("  Nhà bà ngoại ",
                " 12 Lê Lợi, Đà Nẵng ", new BigDecimal("16.0471234"), new BigDecimal("108.2068765"),
                ActivityType.ACCOMMODATION));

        ArgumentCaptor<Place> stored = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(stored.capture());
        assertThat(stored.getValue().getProvider()).isEqualTo(PlaceProvider.MANUAL);
        assertThat(stored.getValue().getExternalId()).isNull();
        assertThat(stored.getValue().getName()).isEqualTo("Nhà bà ngoại");
        assertThat(stored.getValue().getAddress()).isEqualTo("12 Lê Lợi, Đà Nẵng");
        assertThat(stored.getValue().getCategory()).isEqualTo("ACCOMMODATION");
        assertThat(stored.getValue().getCreatedBy()).isSameAs(creator);
        assertThat(response).isEqualTo(new PlaceResponse(40L, PlaceProvider.MANUAL, "Nhà bà ngoại", "12 Lê Lợi, Đà Nẵng",
                new BigDecimal("16.0471234"), new BigDecimal("108.2068765"), "ACCOMMODATION"));
        // Nothing is asked of the map source, and nothing is merged with an existing place
        verifyNoInteractions(mapProvider);
        verify(placeRepository, never()).findByProviderAndExternalId(any(), any());
    }

    @Test
    void manualPlaceWithoutAddressOrCategoryLeavesThemEmpty() {
        when(userRepository.getReferenceById(7L)).thenReturn(TestUsers.verified(7L, "an@example.com"));
        when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 41L));

        placeService.createManual(7L, new CreateManualPlaceRequest("Điểm hẹn", "   ", new BigDecimal("16"),
                new BigDecimal("108"), null));

        ArgumentCaptor<Place> stored = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(stored.capture());
        // A blank address is "no address", not a string of spaces
        assertThat(stored.getValue().getAddress()).isNull();
        assertThat(stored.getValue().getCategory()).isNull();
    }

    // ---------- findAttachable ----------

    @Test
    void placeCopiedFromASourceCanBeAttachedByAnyone() {
        Place linhUng = storedLinhUng(31L);
        when(placeRepository.findById(31L)).thenReturn(Optional.of(linhUng));

        assertThat(placeService.findAttachable(31L, 7L)).isSameAs(linhUng);
        assertThat(placeService.findAttachable(31L, 8L)).isSameAs(linhUng);
    }

    @Test
    void manualPlaceCanBeAttachedByItsCreator() {
        Place home = manualPlaceOf(7L);
        when(placeRepository.findById(40L)).thenReturn(Optional.of(home));

        assertThat(placeService.findAttachable(40L, 7L)).isSameAs(home);
    }

    @Test
    void manualPlaceOfSomeoneElseLooksExactlyLikeAPlaceThatDoesNotExist() {
        Place home = manualPlaceOf(7L);
        when(placeRepository.findById(40L)).thenReturn(Optional.of(home));
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());

        Throwable someoneElses = catchThrowable(() -> placeService.findAttachable(40L, 8L));
        Throwable unknown = catchThrowable(() -> placeService.findAttachable(999L, 8L));

        for (Throwable thrown : List.of(someoneElses, unknown)) {
            assertThat(thrown).isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                assertThat(ex.getDetails())
                        .containsExactly(FieldViolation.of("placeId", "error.activity.place-not-found"));
            });
        }
    }

    private static Place manualPlaceOf(Long creatorId) {
        return withId(Place.builder().provider(PlaceProvider.MANUAL).name("Nhà bà ngoại")
                .lat(new BigDecimal("16.0471234")).lng(new BigDecimal("108.2068765"))
                .createdBy(TestUsers.verified(creatorId, "creator" + creatorId + "@example.com")).build(), 40L);
    }

    private static Place storedLinhUng(Long id) {
        return withId(Place.builder()
                .provider(LINH_UNG.provider())
                .externalId(LINH_UNG.externalId())
                .name(LINH_UNG.name())
                .address(LINH_UNG.address())
                .lat(LINH_UNG.lat())
                .lng(LINH_UNG.lng())
                .category(LINH_UNG.category())
                .build(), id);
    }

    /** Sets the id the database would assign; BaseEntity deliberately has no setter for it. */
    private static Place withId(Place place, Long id) {
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

}
