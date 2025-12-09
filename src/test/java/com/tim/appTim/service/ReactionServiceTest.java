package com.tim.appTim.service;

import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.entity.*;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReplyCommentRepository replyCommentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GamificationService gamificationService;
    @Mock
    private BehaviorLookupService behaviorLookupService;

    @InjectMocks
    private ReactionService reactionService;

    private User user;
    private Post post;
    private Comment comment;
    private ReplyComment replyComment;
    private Reaction reaction;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setProfileImage("avatar.jpg");

        post = new Post();
        post.setId(100L);
        post.setUser(user);
        post.setTotalReactions(0);

        comment = new Comment();
        comment.setId(10L);
        comment.setPost(post);
        comment.setUser(user);

        replyComment = new ReplyComment();
        replyComment.setId(20L);
        replyComment.setComment(comment);
        replyComment.setUser(user);

        reaction = new Reaction();
        reaction.setId(1L);
        reaction.setUser(user);
        reaction.setPost(post);
        reaction.setEmotionType(Reaction.EmotionType.like);
        reaction.setCreatedAt(LocalDateTime.now());
    }

    // --- createOrUpdateReaction (Post) ---

    @Test
    // Covers: Post Not Found
    void createOrUpdateReaction_PostNotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.createOrUpdateReaction(100L, 1L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    // Covers: User Not Found
    void createOrUpdateReaction_UserNotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.createOrUpdateReaction(100L, 1L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    // Covers: Create New Reaction (Success)
    void createOrUpdateReaction_CreateNew() {
        User otherUser = new User();
        otherUser.setId(2L);
        post.setUser(otherUser); // Trigger notification

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(post, user))
                .thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(i -> i.getArgument(0));
        when(reactionRepository.countByPostAndCommentIsNullAndReplyCommentIsNull(post)).thenReturn(1L);

        ReactionDTO result = reactionService.createOrUpdateReaction(100L, 1L, Reaction.EmotionType.love);

        assertThat(result).isNotNull();
        assertThat(result.getEmotionType()).isEqualTo("love");
        verify(notificationService).createReactionNotification(eq(2L), isNull(), isNull(), eq(1L), eq("testuser"),
                eq(Notification.NotificationType.POST_REACTION), eq("POST"), eq(100L));
    }

    @Test
    // Covers: Update Existing Reaction (Success)
    void createOrUpdateReaction_UpdateExisting() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(post, user))
                .thenReturn(Optional.of(reaction));
        when(reactionRepository.save(any(Reaction.class))).thenReturn(reaction);
        when(reactionRepository.countByPostAndCommentIsNullAndReplyCommentIsNull(post)).thenReturn(1L);

        ReactionDTO result = reactionService.createOrUpdateReaction(100L, 1L, Reaction.EmotionType.haha);

        assertThat(result.getEmotionType()).isEqualTo("haha");
    }

    @Test
    // Covers: Gamification Trigger
    void createOrUpdateReaction_Gamification() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(post, user))
                .thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenReturn(reaction);
        when(reactionRepository.countByPostAndCommentIsNullAndReplyCommentIsNull(post)).thenReturn(10L); // >= 10
        when(behaviorLookupService.getIdByName("POST'S_LIKE")).thenReturn(1);

        reactionService.createOrUpdateReaction(100L, 1L, Reaction.EmotionType.like);

        verify(gamificationService).awardPoints(eq(1L), eq(1));
    }

    // --- createOrUpdateCommentReaction ---

    @Test
    // Covers: Comment Not Found
    void createOrUpdateCommentReaction_CommentNotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.createOrUpdateCommentReaction(10L, 1L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found");
    }

    @Test
    // Covers: User Not Found
    void createOrUpdateCommentReaction_UserNotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.createOrUpdateCommentReaction(10L, 1L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    // Covers: Success (Create New)
    void createOrUpdateCommentReaction_Success() {
        User otherUser = new User();
        otherUser.setId(2L);
        comment.setUser(otherUser); // Trigger notification

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByCommentAndUserAndReplyCommentIsNull(comment, user)).thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(i -> i.getArgument(0));

        ReactionDTO result = reactionService.createOrUpdateCommentReaction(10L, 1L, Reaction.EmotionType.like);

        assertThat(result).isNotNull();
        verify(notificationService).createReactionNotification(isNull(), eq(2L), isNull(), eq(1L), eq("testuser"),
                eq(Notification.NotificationType.COMMENT_REACTION), eq("COMMENT"), eq(10L));
    }

    // --- createOrUpdateReplyCommentReaction ---

    @Test
    // Covers: Reply Not Found
    void createOrUpdateReplyCommentReaction_NotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.createOrUpdateReplyCommentReaction(20L, 1L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: User Not Found
    void createOrUpdateReplyCommentReaction_UserNotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.createOrUpdateReplyCommentReaction(20L, 1L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success (Create New)
    void createOrUpdateReplyCommentReaction_Success() {
        User otherUser = new User();
        otherUser.setId(2L);
        replyComment.setUser(otherUser); // Trigger notification

        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByReplyCommentAndUser(replyComment, user)).thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(i -> i.getArgument(0));

        ReactionDTO result = reactionService.createOrUpdateReplyCommentReaction(20L, 1L, Reaction.EmotionType.like);

        assertThat(result).isNotNull();
        verify(notificationService).createReactionNotification(isNull(), isNull(), eq(2L), eq(1L), eq("testuser"),
                eq(Notification.NotificationType.REPLY_REACTION), eq("REPLY"), eq(20L));
    }

    // --- getReactionsByPostId ---

    @Test
    // Covers: Post Not Found
    void getReactionsByPostId_NotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.getReactionsByPostId(100L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void getReactionsByPostId_Success() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        Page<Reaction> page = new PageImpl<>(Collections.singletonList(reaction));
        when(reactionRepository.findByPostAndCommentIsNullAndReplyCommentIsNull(eq(post), any(Pageable.class)))
                .thenReturn(page);

        Page<ReactionDTO> result = reactionService.getReactionsByPostId(100L, PageRequest.of(0, 10));
        assertThat(result).isNotEmpty();
    }

    // --- getReactionsByCommentId ---

    @Test
    // Covers: Comment Not Found
    void getReactionsByCommentId_NotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.getReactionsByCommentId(10L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void getReactionsByCommentId_Success() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        Page<Reaction> page = new PageImpl<>(Collections.singletonList(reaction));
        when(reactionRepository.findByCommentAndReplyCommentIsNull(eq(comment), any(Pageable.class))).thenReturn(page);

        Page<ReactionDTO> result = reactionService.getReactionsByCommentId(10L, PageRequest.of(0, 10));
        assertThat(result).isNotEmpty();
    }

    // --- getReactionsByReplyCommentId ---

    @Test
    // Covers: Reply Not Found
    void getReactionsByReplyCommentId_NotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.getReactionsByReplyCommentId(20L, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void getReactionsByReplyCommentId_Success() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        Page<Reaction> page = new PageImpl<>(Collections.singletonList(reaction));
        when(reactionRepository.findByReplyComment(eq(replyComment), any(Pageable.class))).thenReturn(page);

        Page<ReactionDTO> result = reactionService.getReactionsByReplyCommentId(20L, PageRequest.of(0, 10));
        assertThat(result).isNotEmpty();
    }

    // --- deleteReaction (Post) ---

    @Test
    // Covers: Post Not Found
    void deleteReaction_PostNotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.deleteReaction(100L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: User Not Found
    void deleteReaction_UserNotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.deleteReaction(100L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void deleteReaction_Success() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByPostAndUserAndCommentIsNullAndReplyCommentIsNull(post, user))
                .thenReturn(Optional.of(reaction));

        reactionService.deleteReaction(100L, 1L);
        verify(reactionRepository).delete(reaction);
    }

    // --- deleteCommentReaction ---

    @Test
    // Covers: Comment Not Found
    void deleteCommentReaction_NotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.deleteCommentReaction(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: User Not Found
    void deleteCommentReaction_UserNotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.deleteCommentReaction(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void deleteCommentReaction_Success() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByCommentAndUserAndReplyCommentIsNull(comment, user))
                .thenReturn(Optional.of(reaction));

        reactionService.deleteCommentReaction(10L, 1L);
        verify(reactionRepository).delete(reaction);
    }

    // --- deleteReplyCommentReaction ---

    @Test
    // Covers: Reply Not Found
    void deleteReplyCommentReaction_NotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.deleteReplyCommentReaction(20L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: User Not Found
    void deleteReplyCommentReaction_UserNotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.deleteReplyCommentReaction(20L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void deleteReplyCommentReaction_Success() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reactionRepository.findByReplyCommentAndUser(replyComment, user)).thenReturn(Optional.of(reaction));

        reactionService.deleteReplyCommentReaction(20L, 1L);
        verify(reactionRepository).delete(reaction);
    }

    // --- countReactionsByType ---

    @Test
    // Covers: Post Not Found
    void countReactionsByType_NotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.countReactionsByType(100L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void countReactionsByType_Success() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(reactionRepository.countByPostAndEmotionType(post, Reaction.EmotionType.like)).thenReturn(5L);
        assertThat(reactionService.countReactionsByType(100L, Reaction.EmotionType.like)).isEqualTo(5L);
    }

    // --- countCommentReactionsByType ---

    @Test
    // Covers: Comment Not Found
    void countCommentReactionsByType_NotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.countCommentReactionsByType(10L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void countCommentReactionsByType_Success() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(reactionRepository.countByCommentAndEmotionType(comment, Reaction.EmotionType.like)).thenReturn(3L);
        assertThat(reactionService.countCommentReactionsByType(10L, Reaction.EmotionType.like)).isEqualTo(3L);
    }

    // --- countReplyCommentReactionsByType ---

    @Test
    // Covers: Reply Not Found
    void countReplyCommentReactionsByType_NotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reactionService.countReplyCommentReactionsByType(20L, Reaction.EmotionType.like))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void countReplyCommentReactionsByType_Success() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        when(reactionRepository.countByReplyCommentAndEmotionType(replyComment, Reaction.EmotionType.like))
                .thenReturn(2L);
        assertThat(reactionService.countReplyCommentReactionsByType(20L, Reaction.EmotionType.like)).isEqualTo(2L);
    }
}
