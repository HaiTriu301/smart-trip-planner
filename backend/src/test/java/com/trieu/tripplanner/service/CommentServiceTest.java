package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.request.CreateCommentRequest;
import com.trieu.tripplanner.dto.response.CommentResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.CommentMapper;
import com.trieu.tripplanner.model.Comment;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.CommentRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Repositories are mocks; the MapStruct mapper is the real generated one. Who may call these methods at all is the
 * job of the controller guard and is tested there and in TripPermissionMatrixIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private static final long TRIP_ID = 5L;
    private static final long OWNER_ID = 7L;
    private static final long VIEWER_ID = 8L;
    private static final long COMMENT_ID = 51L;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    private CommentService commentService;
    private Trip trip;
    private User viewer;

    @BeforeEach
    void setUp() {
        User owner = TestUsers.verified(OWNER_ID, "owner@example.com");
        viewer = TestUsers.verified(VIEWER_ID, "viewer@example.com");
        trip = Trip.builder()
                .owner(owner)
                .title("Đà Lạt mùa hoa")
                .slug("da-lat-mua-hoa-x7k2qp")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 3))
                .build();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        commentService = new CommentService(commentRepository, tripRepository, userRepository,
                Mappers.getMapper(CommentMapper.class));
    }

    @Nested
    class Create {

        @Test
        void postsATrimmedRootCommentOnTheWholeTripWrittenByTheCaller() {
            tripExists();
            when(userRepository.getReferenceById(VIEWER_ID)).thenReturn(viewer);
            saveAssignsId();

            CommentResponse response = commentService.create(TRIP_ID, VIEWER_ID,
                    new CreateCommentRequest("  Nhớ mang áo ấm  "));

            Comment saved = savedComment();
            assertThat(saved.getTrip()).isSameAs(trip);
            assertThat(saved.getAuthor()).isSameAs(viewer);
            assertThat(saved.getContent()).isEqualTo("Nhớ mang áo ấm");
            assertThat(saved.getActivity()).isNull();
            assertThat(saved.getParent()).isNull();
            assertThat(response.id()).isEqualTo(COMMENT_ID);
            assertThat(response.activityId()).isNull();
            assertThat(response.parentId()).isNull();
            assertThat(response.author()).isEqualTo(
                    new CommentResponse.Author(VIEWER_ID, "Test viewer@example.com", null));
            assertThat(response.content()).isEqualTo("Nhớ mang áo ấm");
        }

        @Test
        void keepsTheLineBreaksInsideTheContent() {
            tripExists();
            when(userRepository.getReferenceById(VIEWER_ID)).thenReturn(viewer);
            saveAssignsId();

            commentService.create(TRIP_ID, VIEWER_ID, new CreateCommentRequest("\n  Sáng: chợ\nChiều: hồ  \n"));

            // Only the two ends are trimmed: the text is shown as written
            assertThat(savedComment().getContent()).isEqualTo("Sáng: chợ\nChiều: hồ");
        }

        @Test
        void aTripThatDoesNotExistOrWasDeletedIs404AndNothingIsSaved() {
            // A soft-deleted trip is invisible to findById (@SQLRestriction), so both cases look the same here
            when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.create(TRIP_ID, VIEWER_ID, new CreateCommentRequest("Xin chào")))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(commentRepository, never()).save(any());
        }

    }

    private void tripExists() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
    }

    /** What the database does on INSERT: the row gets its id. */
    private void saveAssignsId() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", COMMENT_ID);
            return comment;
        });
    }

    private Comment savedComment() {
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        return captor.getValue();
    }

}
