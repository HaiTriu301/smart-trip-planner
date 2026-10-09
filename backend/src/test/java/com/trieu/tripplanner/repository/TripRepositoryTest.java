package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.internal.TripStatusCount;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.repository.spec.TripSpecifications;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    void findAccessGivesTheOwnerWithoutARoleForOwnerAndStranger() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Đà Lạt", "da-lat-aaaaaa", OCT_1, OCT_1.plusDays(2)));

        assertThat(tripRepository.findAccess(trip.getId(), owner.getId())).contains(new TripAccess(owner.getId(), null));
        assertThat(tripRepository.findAccess(trip.getId(), stranger.getId())).contains(new TripAccess(owner.getId(), null));
    }

    @Test
    void findAccessGivesTheRoleOfAnAcceptedMemberOnly() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Đà Lạt", "da-lat-aaaaaa", OCT_1, OCT_1.plusDays(2)));
        User editor = entityManager.persist(user("editor@example.com"));
        User pending = entityManager.persist(user("pending@example.com"));
        User removed = entityManager.persist(user("removed@example.com"));
        entityManager.persist(member(trip, editor, MemberRole.EDITOR, MemberStatus.ACCEPTED));
        entityManager.persist(member(trip, pending, MemberRole.EDITOR, MemberStatus.PENDING));
        entityManager.persist(member(trip, removed, MemberRole.EDITOR, MemberStatus.REMOVED));
        entityManager.persist(member(trip, stranger, MemberRole.VIEWER, MemberStatus.ACCEPTED));
        entityManager.flush();

        assertThat(tripRepository.findAccess(trip.getId(), editor.getId()))
                .contains(new TripAccess(owner.getId(), MemberRole.EDITOR));
        assertThat(tripRepository.findAccess(trip.getId(), stranger.getId()))
                .contains(new TripAccess(owner.getId(), MemberRole.VIEWER));
        // Not accepted = no role, exactly like somebody who was never invited
        assertThat(tripRepository.findAccess(trip.getId(), pending.getId())).contains(new TripAccess(owner.getId(), null));
        assertThat(tripRepository.findAccess(trip.getId(), removed.getId())).contains(new TripAccess(owner.getId(), null));
    }

    @Test
    void findAccessDoesNotMixUpTheMembershipsOfTwoTrips() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Đà Lạt", "da-lat-aaaaaa", OCT_1, OCT_1));
        Trip other = tripRepository.saveAndFlush(trip(owner, "Huế", "hue-bbbbbb", OCT_1, OCT_1));
        entityManager.persist(member(trip, stranger, MemberRole.EDITOR, MemberStatus.ACCEPTED));
        entityManager.flush();

        assertThat(tripRepository.findAccess(other.getId(), stranger.getId())).contains(new TripAccess(owner.getId(), null));
    }

    @Test
    void findAccessIsEmptyForDeletedOrMissingTrip() {
        Trip trip = tripRepository.saveAndFlush(trip(owner, "Huế", "hue-bbbbbb", OCT_1, OCT_1));
        entityManager.persist(member(trip, stranger, MemberRole.EDITOR, MemberStatus.ACCEPTED));
        entityManager.flush();
        // Forget the member first: a managed row pointing at a removed entity makes the flush fail
        entityManager.clear();
        tripRepository.delete(tripRepository.findById(trip.getId()).orElseThrow());
        tripRepository.flush();

        // The membership row still exists, but a deleted trip is gone for everybody, members included
        assertThat(tripRepository.findAccess(trip.getId(), owner.getId())).isEmpty();
        assertThat(tripRepository.findAccess(trip.getId(), stranger.getId())).isEmpty();
        assertThat(tripRepository.findAccess(999_999L, owner.getId())).isEmpty();
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
    void matchingReturnsOwnTripsAndTripsSharedWithMeAsAnAcceptedMember() {
        tripRepository.save(trip(owner, "Của tôi", "cua-toi-dddddd", OCT_1, OCT_1));
        Trip shared = tripRepository.save(trip(stranger, "Được chia sẻ", "duoc-chia-se-eeeeee", OCT_1, OCT_1));
        Trip invited = tripRepository.save(trip(stranger, "Mới được mời", "moi-duoc-moi-tttttt", OCT_1, OCT_1));
        Trip left = tripRepository.save(trip(stranger, "Đã bị gỡ", "da-bi-go-uuuuuu", OCT_1, OCT_1));
        tripRepository.save(trip(stranger, "Của người khác", "cua-nguoi-khac-vvvvvv", OCT_1, OCT_1));
        entityManager.persist(member(shared, owner, MemberRole.VIEWER, MemberStatus.ACCEPTED));
        entityManager.persist(member(invited, owner, MemberRole.EDITOR, MemberStatus.PENDING));
        entityManager.persist(member(left, owner, MemberRole.EDITOR, MemberStatus.REMOVED));
        entityManager.flush();

        // Only an accepted membership opens a trip; the other filters apply to shared trips like to own ones
        assertThat(titles(new TripFilter(null, null, null, null))).containsExactly("Của tôi", "Được chia sẻ");
        assertThat(titles(new TripFilter(null, "chia sẻ", null, null))).containsExactly("Được chia sẻ");
        // The stranger's own list is untouched by the memberships they handed out
        assertThat(tripRepository.findAll(TripSpecifications.matching(stranger.getId(),
                new TripFilter(null, null, null, null)), Sort.by("title")))
                .extracting(Trip::getTitle).doesNotContain("Của tôi").hasSize(4);
    }

    @Test
    void withOwnerLoadsTheOwnerInTheListQueryAndStaysOutOfTheCountQueries() {
        tripRepository.save(trip(owner, "Của tôi", "cua-toi-dddddd", OCT_1, OCT_1));
        Trip shared = tripRepository.save(trip(stranger, "Được chia sẻ", "duoc-chia-se-eeeeee", OCT_1, OCT_1));
        entityManager.persist(member(shared, owner, MemberRole.VIEWER, MemberStatus.ACCEPTED));
        entityManager.flush();
        entityManager.clear();
        Specification<Trip> spec = TripSpecifications.matching(owner.getId(), new TripFilter(null, null, null, null))
                .and(TripSpecifications.withOwner());

        // A full page of one makes Spring Data run its COUNT query too: the fetch must not be applied there
        Page<Trip> page = tripRepository.findAll(spec, PageRequest.of(0, 1, Sort.by("title").descending()));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).singleElement().satisfies(trip -> {
            assertThat(trip.getTitle()).isEqualTo("Được chia sẻ");
            // Already loaded: the mapper reads the name without a second SELECT (CLAUDE.md section 8, N+1)
            assertThat(Hibernate.isInitialized(trip.getOwner())).isTrue();
            assertThat(trip.getOwner().getFullName()).isEqualTo("Test stranger@example.com");
        });
        // Same for the status grouping, which selects (status, count) and not trips
        assertThat(tripRepository.countByStatus(spec)).containsExactly(new TripStatusCount(TripStatus.DRAFT, 2));
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

    // ---- countByStatus (status chips) -------------------------------------------------------------------------

    @Test
    void countByStatusGroupsLiveTripsOfTheOwnerAndSharedWithThemAndHonoursTheKeyword() {
        tripRepository.save(trip(owner, "Nháp 1", "nhap-1-oooooo", OCT_1, OCT_1));
        tripRepository.save(trip(owner, "Nháp 2", "nhap-2-pppppp", OCT_1, OCT_1));
        Trip planned = trip(owner, "Nghỉ lễ", "nghi-le-qqqqqq", OCT_1, OCT_1);
        planned.setStatus(TripStatus.PLANNED);
        planned.setDestinationName("Vũng Tàu");
        tripRepository.save(planned);
        // Shared with the owner by somebody else: counted like their own (the chips match the list)
        Trip shared = trip(stranger, "Chia sẻ", "chia-se-wwwwww", OCT_1, OCT_1);
        shared.setStatus(TripStatus.PLANNED);
        entityManager.persist(member(tripRepository.save(shared), owner, MemberRole.VIEWER, MemberStatus.ACCEPTED));
        Trip deleted = trip(owner, "Đã xoá", "da-xoa-rrrrrr", OCT_1, OCT_1);
        deleted.setStatus(TripStatus.PLANNED);
        tripRepository.delete(tripRepository.saveAndFlush(deleted));
        tripRepository.save(trip(stranger, "Của người khác", "nguoi-khac-ssssss", OCT_1, OCT_1));
        tripRepository.flush();

        assertThat(tripRepository.countByStatus(TripSpecifications.matching(owner.getId(),
                new TripFilter(null, null, null, null))))
                .containsExactlyInAnyOrder(new TripStatusCount(TripStatus.DRAFT, 2),
                        new TripStatusCount(TripStatus.PLANNED, 2));
        // Keyword on the destination, case- and accent-insensitive like the list
        assertThat(tripRepository.countByStatus(TripSpecifications.matching(owner.getId(),
                new TripFilter(null, "VŨNG TÀU", null, null))))
                .containsExactly(new TripStatusCount(TripStatus.PLANNED, 1));
    }

    private List<String> titles(TripFilter filter) {
        return tripRepository.findAll(TripSpecifications.matching(owner.getId(), filter), Sort.by("title"))
                .stream().map(Trip::getTitle).toList();
    }

    private static TripMember member(Trip trip, User user, MemberRole role, MemberStatus status) {
        return TripMember.builder()
                .trip(trip)
                .user(user)
                .invitedEmail(user.getEmail())
                .role(role)
                .status(status)
                .invitedBy(trip.getOwner())
                .invitedAt(Instant.parse("2026-09-01T00:00:00Z"))
                .build();
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
