package com.trieu.tripplanner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.repository.CommentRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Checks Comment.java against V13__create_comments.sql on a real MySQL (Testcontainers, Flyway,
 * ddl-auto=validate), plus what the comment rules lean on: soft delete, comments disappearing with their activity,
 * day or trip, a root and its replies going together in one hard delete, and comments that cannot be edited.
 * Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CommentMappingTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository repository;

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
        activity = entityManager.persist(activity(day, "Ăn sáng"));
        entityManager.flush();
    }

    @Test
    void everyColumnOfAReplyOnAnActivitySurvivesARoundTrip() {
        Comment root = repository.saveAndFlush(onActivity(owner, "Mấy giờ đi?").build());
        Comment saved = repository.saveAndFlush(onActivity(viewer, "7 giờ nhé").parent(root).build());
        entityManager.clear();

        Comment found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getActivity().getId()).isEqualTo(activity.getId());
        assertThat(found.getAuthor().getId()).isEqualTo(viewer.getId());
        assertThat(found.getParent().getId()).isEqualTo(root.getId());
        assertThat(found.getContent()).isEqualTo("7 giờ nhé");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getDeletedAt()).isNull();
    }

    @Test
    void aRootCommentOnTheWholeTripHasNoActivityAndNoParent() {
        Comment saved = repository.saveAndFlush(onTrip(owner, "Cả nhà nhớ mang áo ấm").build());
        entityManager.clear();

        Comment found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getActivity()).isNull();
        assertThat(found.getParent()).isNull();
    }

    @Test
    void twoThousandCharactersWithAccentsEmojiAndLineBreaksAreKeptAsWritten() {
        // 2000 is the limit of the request (design.md 10.2); TEXT and utf8mb4 must keep every character of it
        String content = ("Đà Lạt 🌲\n").repeat(200);
        assertThat(content).hasSize(2000);
        Comment saved = repository.saveAndFlush(onTrip(owner, content).build());
        entityManager.clear();

        assertThat(repository.findById(saved.getId()).orElseThrow().getContent()).isEqualTo(content);
    }

    @Test
    void deletingThroughTheRepositoryOnlyStampsDeletedAtAndHidesTheComment() {
        Comment saved = repository.saveAndFlush(onTrip(owner, "Xoá tôi").build());

        repository.delete(saved);
        repository.flush();
        entityManager.clear();

        assertThat(repository.findById(saved.getId())).isEmpty();
        // The row is still there, stamped: CLAUDE.md rule 10
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at IS NOT NULL FROM comments WHERE id = ?",
                Boolean.class, saved.getId())).isTrue();
    }

    @Test
    void hardDeletingAnActivityRemovesItsCommentsAndTheirRepliesButNotTheCommentsOfTheTrip() {
        Comment root = repository.saveAndFlush(onActivity(owner, "Quán này ngon không?").build());
        repository.saveAndFlush(onActivity(viewer, "Ngon lắm").parent(root).build());
        Comment onTrip = repository.saveAndFlush(onTrip(owner, "Bình luận của cả chuyến").build());

        // Activities are hard-deleted (design.md 5.2 "activities"); rule 27: their comments go with them
        jdbcTemplate.update("DELETE FROM activities WHERE id = ?", activity.getId());

        assertThat(commentIds()).containsExactly(onTrip.getId());
    }

    @Test
    void hardDeletingADayRemovesTheCommentsOfItsActivities() {
        Comment root = repository.saveAndFlush(onActivity(owner, "Quán này ngon không?").build());
        repository.saveAndFlush(onActivity(viewer, "Ngon lắm").parent(root).build());

        // Cutting the date range with force=true (rule 14.3): trip_days -> activities -> comments
        jdbcTemplate.update("DELETE FROM trip_days WHERE id = ?", day.getId());

        assertThat(commentIds()).isEmpty();
    }

    @Test
    void hardDeletingATripWithRootsAndRepliesOnTheTripAndOnAnActivityRemovesThemAll() {
        Comment onTrip = repository.saveAndFlush(onTrip(owner, "Cả nhà nhớ mang áo ấm").build());
        repository.saveAndFlush(onTrip(viewer, "Ok").parent(onTrip).build());
        Comment onActivity = repository.saveAndFlush(onActivity(owner, "Mấy giờ đi?").build());
        repository.saveAndFlush(onActivity(viewer, "7 giờ").parent(onActivity).build());

        // What the retention scheduler will do to a soft-deleted trip (rule 14.8); bypasses @SQLDelete on purpose.
        // The same rows are reached by two paths (trips -> comments, trips -> trip_days -> activities -> comments)
        // and a root is reached together with its replies: fk_comments_parent must not refuse it
        jdbcTemplate.update("DELETE FROM trips WHERE id = ?", trip.getId());

        assertThat(commentIds()).isEmpty();
    }

    @Test
    void anAuthorWhoStillHasCommentsCannotBeHardDeleted() {
        repository.saveAndFlush(onTrip(viewer, "Bình luận của người xem").build());

        // Users are soft-deleted; a hard delete must not silently wipe what they wrote
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", viewer.getId()))
                .hasStackTraceContaining("fk_comments_user");
    }

    @Test
    void aCommentCannotBeChangedAfterItIsWritten() {
        Trip otherTrip = entityManager.persist(trip("hue-2-ngay-q1w2e3"));
        Comment root = repository.saveAndFlush(onTrip(owner, "Gốc").build());
        Comment saved = repository.saveAndFlush(onActivity(viewer, "Lời gốc").parent(root).build());

        // No setters exist (rule 27: no editing); even a reflective change must not reach the database
        ReflectionTestUtils.setField(saved, "trip", otherTrip);
        ReflectionTestUtils.setField(saved, "activity", null);
        ReflectionTestUtils.setField(saved, "author", owner);
        ReflectionTestUtils.setField(saved, "parent", null);
        ReflectionTestUtils.setField(saved, "content", "Lời đã sửa");
        repository.saveAndFlush(saved);
        entityManager.clear();

        Comment found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getActivity().getId()).isEqualTo(activity.getId());
        assertThat(found.getAuthor().getId()).isEqualTo(viewer.getId());
        assertThat(found.getParent().getId()).isEqualTo(root.getId());
        assertThat(found.getContent()).isEqualTo("Lời gốc");
    }

    /** Ids of every row of the table, deleted ones included: read in SQL, past @SQLRestriction. */
    private List<Long> commentIds() {
        return jdbcTemplate.queryForList("SELECT id FROM comments ORDER BY id", Long.class);
    }

    private Comment.CommentBuilder onTrip(User author, String content) {
        return Comment.builder().trip(trip).author(author).content(content);
    }

    private Comment.CommentBuilder onActivity(User author, String content) {
        return onTrip(author, content).activity(activity);
    }

    private Activity activity(TripDay ofDay, String title) {
        return Activity.builder().tripDay(ofDay).title(title).orderIndex(1000).createdBy(owner).build();
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
