package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Comment;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.User;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real MySQL (Testcontainers): the queries of CommentRepository lean on @SQLRestriction and on conditions over
 * foreign keys, which only a real database can prove. Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CommentRepositoryTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    // Whole microseconds: DATETIME(6) would round anything finer (CLAUDE.md section 8)
    private static final Instant DELETED_AT = Instant.parse("2026-10-18T03:00:00.123456Z");

    @Autowired
    private CommentRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private User viewer;
    private Trip trip;
    private TripDay day;
    private Activity activity;

    @BeforeEach
    void createTripWithOneActivity() {
        owner = entityManager.persist(user("owner@example.com", "Chủ chuyến đi"));
        viewer = entityManager.persist(user("viewer@example.com", "Người xem"));
        trip = entityManager.persist(trip("da-lat-3-ngay-x7k2qp"));
        day = entityManager.persist(TripDay.builder().trip(trip).dayIndex(1).date(OCT_1).build());
        activity = entityManager.persist(Activity.builder().tripDay(day).title("Chợ Đà Lạt").orderIndex(1000)
                .createdBy(owner).build());
        entityManager.flush();
    }

    // ---- findRootByIdAndTripId: what a reply may point to ----------------------------------------------------------

    @Test
    void findsALiveRootCommentOfTheTrip() {
        Comment root = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").activity(activity).build());
        entityManager.clear();

        assertThat(repository.findRootByIdAndTripId(root.getId(), trip.getId()))
                .get().extracting(Comment::getId).isEqualTo(root.getId());
    }

    @Test
    void aReplyIsNotARootSoItCannotBeAnswered() {
        Comment root = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").build());
        Comment reply = repository.saveAndFlush(comment(trip, viewer, "7 giờ nhé").parent(root).build());
        entityManager.clear();

        assertThat(repository.findRootByIdAndTripId(reply.getId(), trip.getId())).isEmpty();
    }

    @Test
    void aDeletedRootCannotBeAnswered() {
        Comment root = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").build());
        repository.delete(root);
        repository.flush();
        entityManager.clear();

        assertThat(repository.findRootByIdAndTripId(root.getId(), trip.getId())).isEmpty();
    }

    @Test
    void aRootOfAnotherTripIsNotFoundThroughThisTrip() {
        Trip otherTrip = entityManager.persist(trip("hue-2-ngay-q1w2e3"));
        Comment rootOfOther = repository.saveAndFlush(comment(otherTrip, owner, "Đi Huế không?").build());
        entityManager.clear();

        assertThat(repository.findRootByIdAndTripId(rootOfOther.getId(), trip.getId())).isEmpty();
        // Same comment through its own trip: the trip is what made the difference
        assertThat(repository.findRootByIdAndTripId(rootOfOther.getId(), otherTrip.getId())).isPresent();
    }

    // ---- findAllByTripIdWithAuthor: the list of a trip -------------------------------------------------------------

    @Test
    void listsRootsAndRepliesFlatOldestFirstAndTheIdBreaksTies() {
        Comment first = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").activity(activity).build());
        Comment earlier = repository.saveAndFlush(comment(trip, viewer, "Cả nhà nhớ áo ấm").build());
        Comment sameInstant = repository.saveAndFlush(comment(trip, viewer, "7 giờ nhé").parent(first)
                .activity(activity).build());
        // Literal times in SQL, read back as UTC like the rest of the schema (CLAUDE.md section 8, BUG-AUTH-008)
        createdAt(first, "2026-10-17 03:00:00");
        createdAt(earlier, "2026-10-17 02:00:00");
        createdAt(sameInstant, "2026-10-17 03:00:00");
        entityManager.clear();

        assertThat(repository.findAllByTripIdWithAuthor(trip.getId())).extracting(Comment::getId)
                .containsExactly(earlier.getId(), first.getId(), sameInstant.getId());
    }

    @Test
    void theListLeavesOutDeletedCommentsAndCommentsOfOtherTrips() {
        Trip otherTrip = entityManager.persist(trip("hue-2-ngay-q1w2e3"));
        Comment kept = repository.saveAndFlush(comment(trip, owner, "Còn đây").build());
        Comment deleted = repository.saveAndFlush(comment(trip, viewer, "Đã xoá").build());
        repository.saveAndFlush(comment(otherTrip, owner, "Của chuyến khác").build());
        repository.delete(deleted);
        repository.flush();
        entityManager.clear();

        assertThat(repository.findAllByTripIdWithAuthor(trip.getId())).extracting(Comment::getId)
                .containsExactly(kept.getId());
    }

    @Test
    void theAuthorComesInTheSameQuery() {
        repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").build());
        repository.saveAndFlush(comment(trip, viewer, "Cả nhà nhớ áo ấm").build());
        entityManager.clear();

        List<Comment> comments = repository.findAllByTripIdWithAuthor(trip.getId());

        // Without the join fetch each author would be a proxy, loaded by one more query per comment
        assertThat(comments).allSatisfy(comment -> assertThat(Hibernate.isInitialized(comment.getAuthor())).isTrue());
        assertThat(comments).extracting(comment -> comment.getAuthor().getFullName())
                .containsExactlyInAnyOrder("Chủ chuyến đi", "Người xem");
    }

    @Test
    void aTripWithoutCommentsGivesAnEmptyList() {
        assertThat(repository.findAllByTripIdWithAuthor(trip.getId())).isEmpty();
    }

    // ---- findAllByTripIdAndActivityIdWithAuthor: the list of one activity -----------------------------------------

    @Test
    void theListOfAnActivityHasItsRootsAndTheirRepliesOldestFirst() {
        Activity otherActivity = entityManager.persist(Activity.builder().tripDay(day).title("Hồ Xuân Hương")
                .orderIndex(2000).createdBy(owner).build());
        Comment later = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").activity(activity).build());
        Comment reply = repository.saveAndFlush(comment(trip, viewer, "7 giờ nhé").parent(later)
                .activity(activity).build());
        Comment earlier = repository.saveAndFlush(comment(trip, viewer, "Chợ có bán dâu").activity(activity).build());
        repository.saveAndFlush(comment(trip, owner, "Cả nhà nhớ áo ấm").build());
        repository.saveAndFlush(comment(trip, owner, "Đi bộ quanh hồ").activity(otherActivity).build());
        Comment deleted = repository.saveAndFlush(comment(trip, viewer, "Đã xoá").activity(activity).build());
        repository.delete(deleted);
        repository.flush();
        createdAt(later, "2026-10-17 03:00:00");
        createdAt(reply, "2026-10-17 04:00:00");
        createdAt(earlier, "2026-10-17 02:00:00");
        entityManager.clear();

        List<Comment> comments = repository.findAllByTripIdAndActivityIdWithAuthor(trip.getId(), activity.getId());

        // Not the whole-trip comment, not the other activity's, not the deleted one
        assertThat(comments).extracting(Comment::getId)
                .containsExactly(earlier.getId(), later.getId(), reply.getId());
        assertThat(comments).allSatisfy(comment -> assertThat(Hibernate.isInitialized(comment.getAuthor())).isTrue());
    }

    // ---- findByIdAndTripId and softDeleteWithReplies: deleting ------------------------------------------------------

    @Test
    void aCommentIsFoundThroughItsOwnTripOnlyAndNotOnceDeleted() {
        Trip otherTrip = entityManager.persist(trip("hue-2-ngay-q1w2e3"));
        Comment comment = repository.saveAndFlush(comment(trip, owner, "Nhớ mang áo ấm").build());
        entityManager.clear();

        assertThat(repository.findByIdAndTripId(comment.getId(), trip.getId())).isPresent();
        assertThat(repository.findByIdAndTripId(comment.getId(), otherTrip.getId())).isEmpty();

        repository.softDeleteWithReplies(comment.getId(), DELETED_AT);
        assertThat(repository.findByIdAndTripId(comment.getId(), trip.getId())).isEmpty();
    }

    @Test
    void deletingARootStampsItAndItsRepliesWithOneInstantAndLeavesTheRestAlone() {
        Comment root = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").build());
        Comment reply = repository.saveAndFlush(comment(trip, viewer, "7 giờ nhé").parent(root).build());
        Comment otherRoot = repository.saveAndFlush(comment(trip, viewer, "Cả nhà nhớ áo ấm").build());
        Comment otherReply = repository.saveAndFlush(comment(trip, owner, "Ừ").parent(otherRoot).build());

        int rows = repository.softDeleteWithReplies(root.getId(), DELETED_AT);

        assertThat(rows).isEqualTo(2);
        assertThat(deletedAt(root)).isEqualTo("2026-10-18 03:00:00.123456");
        assertThat(deletedAt(reply)).isEqualTo("2026-10-18 03:00:00.123456");
        assertThat(deletedAt(otherRoot)).isNull();
        assertThat(deletedAt(otherReply)).isNull();
        assertThat(repository.findAllByTripIdWithAuthor(trip.getId())).extracting(Comment::getId)
                .containsExactly(otherRoot.getId(), otherReply.getId());
    }

    @Test
    void deletingAReplyStampsThatReplyOnly() {
        Comment root = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").build());
        Comment reply = repository.saveAndFlush(comment(trip, viewer, "7 giờ nhé").parent(root).build());
        Comment secondReply = repository.saveAndFlush(comment(trip, owner, "8 giờ").parent(root).build());

        assertThat(repository.softDeleteWithReplies(reply.getId(), DELETED_AT)).isEqualTo(1);

        assertThat(deletedAt(reply)).isNotNull();
        assertThat(deletedAt(root)).isNull();
        assertThat(deletedAt(secondReply)).isNull();
    }

    @Test
    void aReplyDeletedEarlierKeepsItsOwnInstantWhenTheRootGoes() {
        Comment root = repository.saveAndFlush(comment(trip, owner, "Mấy giờ đi chợ?").build());
        Comment reply = repository.saveAndFlush(comment(trip, viewer, "7 giờ nhé").parent(root).build());
        repository.softDeleteWithReplies(reply.getId(), DELETED_AT.minusSeconds(3600));

        assertThat(repository.softDeleteWithReplies(root.getId(), DELETED_AT)).isEqualTo(1);

        assertThat(deletedAt(reply)).isEqualTo("2026-10-18 02:00:00.123456");
        assertThat(deletedAt(root)).isEqualTo("2026-10-18 03:00:00.123456");
    }

    /** deleted_at as MySQL holds it (UTC), read past @SQLRestriction; null while the comment is live. */
    private String deletedAt(Comment comment) {
        return jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(deleted_at, '%Y-%m-%d %H:%i:%s.%f') FROM comments WHERE id = ?", String.class,
                comment.getId());
    }

    private void createdAt(Comment comment, String utc) {
        jdbcTemplate.update("UPDATE comments SET created_at = ? WHERE id = ?", utc, comment.getId());
    }

    private static Comment.CommentBuilder comment(Trip onTrip, User author, String content) {
        return Comment.builder().trip(onTrip).author(author).content(content);
    }

    private Trip trip(String slug) {
        return Trip.builder()
                .owner(owner)
                .title("Chuyến đi thử")
                .slug(slug)
                .startDate(OCT_1)
                .endDate(OCT_1.plusDays(2))
                .build();
    }

    private static User user(String email, String fullName) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName(fullName)
                .build();
    }

}
