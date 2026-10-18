package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Comment;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.User;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
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

    @Autowired
    private CommentRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;
    private User viewer;
    private Trip trip;
    private Activity activity;

    @BeforeEach
    void createTripWithOneActivity() {
        owner = entityManager.persist(user("owner@example.com", "Chủ chuyến đi"));
        viewer = entityManager.persist(user("viewer@example.com", "Người xem"));
        trip = entityManager.persist(trip("da-lat-3-ngay-x7k2qp"));
        TripDay day = entityManager.persist(TripDay.builder().trip(trip).dayIndex(1).date(OCT_1).build());
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
