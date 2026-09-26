package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.repository.spec.TripSpecifications;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real MySQL (Testcontainers): the custom queries and the Specification filters are SQL that only a real
 * database can prove. Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripRepositoryTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;
    private User stranger;

    @BeforeEach
    void createUsers() {
        owner = entityManager.persist(user("owner@example.com"));
        stranger = entityManager.persist(user("stranger@example.com"));
    }

    @Test
    void findOwnerIdByIdReturnsOwnerOfLiveTrip() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Đà Lạt", "da-lat-aaaaaa", OCT_1, OCT_1.plusDays(2)));

        assertThat(tripRepository.findOwnerIdById(trip.getId())).contains(owner.getId());
    }

    @Test
    void findOwnerIdByIdIsEmptyForDeletedOrMissingTrip() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Huế", "hue-bbbbbb", OCT_1, OCT_1));
        tripRepository.delete(trip);
        tripRepository.flush();

        assertThat(tripRepository.findOwnerIdById(trip.getId())).isEmpty();
        assertThat(tripRepository.findOwnerIdById(999_999L)).isEmpty();
    }

    @Test
    void countBySlugIncludingDeletedStillSeesSoftDeletedTrips() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Sa Pa", "sa-pa-cccccc", OCT_1, OCT_1));
        tripRepository.delete(trip);
        tripRepository.flush();

        // The slug is still in the UNIQUE key, so it must still count as taken
        assertThat(tripRepository.countBySlugIncludingDeleted("sa-pa-cccccc")).isEqualTo(1);
        assertThat(tripRepository.countBySlugIncludingDeleted("never-used")).isZero();
    }

    @Test
    void matchingReturnsOnlyTheOwnersTrips() {
        tripRepository.save(trip(owner, "Của tôi", "cua-toi-dddddd", OCT_1, OCT_1));
        tripRepository.save(trip(stranger, "Của người khác", "cua-nguoi-khac-eeeeee", OCT_1, OCT_1));
        tripRepository.flush();

        assertThat(titles(new TripFilter(null, null, null, null))).containsExactly("Của tôi");
    }

    @Test
    void matchingFiltersByStatusAndKeywordInTitleOrDestination() {
        Trip planned = trip(owner, "Nghỉ lễ", "nghi-le-ffffff", OCT_1, OCT_1);
        planned.setStatus(TripStatus.PLANNED);
        planned.setDestinationName("Vũng Tàu");
        tripRepository.save(planned);
        tripRepository.save(trip(owner, "vung tau cuoi tuan", "vung-tau-gggggg", OCT_1, OCT_1));
        tripRepository.save(trip(owner, "Hà Giang", "ha-giang-hhhhhh", OCT_1, OCT_1));
        tripRepository.flush();

        // Case-insensitive, matches title of one trip and destination of the other
        assertThat(titles(new TripFilter(null, "VŨNG TÀU", null, null)))
                .containsExactlyInAnyOrder("Nghỉ lễ", "vung tau cuoi tuan");
        assertThat(titles(new TripFilter(TripStatus.PLANNED, null, null, null))).containsExactly("Nghỉ lễ");
    }

    @Test
    void keywordWildcardsAreMatchedLiterally() {
        tripRepository.save(trip(owner, "Giảm 50% vé", "giam-50-ve-iiiiii", OCT_1, OCT_1));
        tripRepository.save(trip(owner, "Giảm 500 vé", "giam-500-ve-jjjjjj", OCT_1, OCT_1));
        tripRepository.flush();

        assertThat(titles(new TripFilter(null, "50%", null, null))).containsExactly("Giảm 50% vé");
    }

    @Test
    void fromToKeepsTripsWhoseRangeOverlaps() {
        tripRepository.save(trip(owner, "Cuối tháng 9", "cuoi-thang-9-kkkkkk",
                LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 3)));   // overlaps October
        tripRepository.save(trip(owner, "Giữa tháng 10", "giua-thang-10-llllll",
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 17)));
        tripRepository.save(trip(owner, "Tháng 9", "thang-9-mmmmmm",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)));     // ends before October
        tripRepository.flush();

        assertThat(titles(new TripFilter(null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))))
                .containsExactlyInAnyOrder("Cuối tháng 9", "Giữa tháng 10");
    }

    @Test
    void matchingHidesSoftDeletedTrips() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Đã xoá", "da-xoa-nnnnnn", OCT_1, OCT_1));
        tripRepository.delete(trip);
        tripRepository.flush();

        assertThat(titles(new TripFilter(null, null, null, null))).isEmpty();
    }

    private List<String> titles(TripFilter filter) {
        return tripRepository.findAll(TripSpecifications.matching(owner.getId(), filter), Sort.by("title"))
                .stream().map(Trip::getTitle).toList();
    }

    private static User user(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Test " + email)
                .build();
    }

    private static Trip trip(User owner, String title, String slug, LocalDate start, LocalDate end) {
        return Trip.builder()
                .owner(owner)
                .title(title)
                .slug(slug)
                .startDate(start)
                .endDate(end)
                .build();
    }

}
