package com.tim.appTim.service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.FileRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.UserRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReplyCommentRepository replyCommentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private Post post;
    private Comment comment;
    private ReplyComment replyComment;
    private File file;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setProfileImage("avatar.jpg");

        post = new Post();
        post.setId(100L);
        post.setUser(user);
        post.setTotalComments(0);

        comment = new Comment();
        comment.setId(10L);
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent("Test Comment");
        comment.setCreatedAt(LocalDateTime.now());
        comment.setReplies(new ArrayList<>());
        comment.setFiles(new ArrayList<>());

        replyComment = new ReplyComment();
        replyComment.setId(20L);
        replyComment.setComment(comment);
        replyComment.setUser(user);
        replyComment.setContent("Test Reply");
        replyComment.setCreatedAt(LocalDateTime.now());
        replyComment.setFiles(new ArrayList<>());

        file = new File();
        file.setId(5);
        file.setFileUrl("url");
        file.setFileType(File.FileType.IMAGE);
        file.setFileName("test.jpg");
        file.setFileSize(100L);
    }

    // --- createComment ---

    @Test
    // Covers: BadRequest (No content or files)
    void createComment_BadRequest() {
        assertThatThrownBy(() -> commentService.createComment(100L, 1L, "", null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Bạn phải cung cấp nội dung hoặc tệp đính kèm");
    }

    @Test
    // Covers: Post Not Found
    void createComment_PostNotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.createComment(100L, 1L, "Content", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    @Test
    // Covers: User Not Found
    void createComment_UserNotFound() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.createComment(100L, 1L, "Content", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    // Covers: Success (With files and notification)
    void createComment_Success() {
        User otherUser = new User();
        otherUser.setId(2L);
        post.setUser(otherUser); // Trigger notification logic

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenAnswer(i -> {
            Comment c = i.getArgument(0);
            c.setId(10L);
            return c;
        });

        List<File> files = Collections.singletonList(file);
        CommentDTO result = commentService.createComment(100L, 1L, "Content", Comment.Emotion.like, files);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Content");
        verify(notificationService).createCommentNotification(eq(2L), isNull(), eq(1L), eq("testuser"), eq("POST"),
                eq(100L));
    }

    // --- getCommentsByPostId ---

    @Test
    // Covers: Success
    void getCommentsByPostId_Success() {
        Page<Comment> page = new PageImpl<>(Collections.singletonList(comment));
        when(commentRepository.findByPostId(eq(100L), any(Pageable.class))).thenReturn(page);

        Page<CommentDTO> result = commentService.getCommentsByPostId(100L, PageRequest.of(0, 10));
        assertThat(result).isNotEmpty();
    }

    // --- getCommentById ---

    @Test
    // Covers: Not Found
    void getCommentById_NotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.getCommentById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void getCommentById_Success() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        Comment result = commentService.getCommentById(10L);
        assertThat(result).isEqualTo(comment);
    }

    // --- getReplyCommentById ---

    @Test
    // Covers: Not Found
    void getReplyCommentById_NotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.getReplyCommentById(20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void getReplyCommentById_Success() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        ReplyComment result = commentService.getReplyCommentById(20L);
        assertThat(result).isEqualTo(replyComment);
    }

    // --- updateComment ---

    @Test
    // Covers: BadRequest
    void updateComment_BadRequest() {
        assertThatThrownBy(() -> commentService.updateComment(10L, user, authentication, "", null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers: Not Found
    void updateComment_NotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.updateComment(10L, user, authentication, "New", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Forbidden
    void updateComment_Forbidden() {
        User otherUser = new User();
        otherUser.setId(2L);

        // Make post owner different too
        User postOwner = new User();
        postOwner.setId(3L);
        post.setUser(postOwner);

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThatThrownBy(() -> commentService.updateComment(10L, otherUser, authentication, "New", null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    // Covers: Success (Admin, Update Files)
    void updateComment_Success() {
        User otherUser = new User();
        otherUser.setId(2L);

        // Mock Admin Authority
        List<GrantedAuthority> authorities = Collections
                .singletonList(new SimpleGrantedAuthority("comment:update_all"));
        doReturn(authorities).when(authentication).getAuthorities();

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        List<File> newFiles = Collections.singletonList(file);
        CommentDTO result = commentService.updateComment(10L, otherUser, authentication, "Updated",
                Comment.Emotion.love, newFiles);

        assertThat(result.getContent()).isEqualTo("Updated");
        verify(fileRepository).deleteAll(any());
    }
    // --- deleteComment ---

    @Test
    // Covers: Not Found
    void deleteComment_NotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.deleteComment(10L, user, authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Forbidden
    void deleteComment_Forbidden() {
        User otherUser = new User();
        otherUser.setId(2L);
        User postOwner = new User();
        postOwner.setId(3L);
        post.setUser(postOwner);

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThatThrownBy(() -> commentService.deleteComment(10L, otherUser, authentication))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    // Covers: Success (Owner)
    void deleteComment_Success() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        commentService.deleteComment(10L, user, authentication);

        verify(commentRepository).delete(comment);
        assertThat(post.getTotalComments()).isEqualTo(0); // Should decrease (0 -> 0 handled, but logic says
                                                          // currentTotal - 1 if > 0)
    }

    // --- createReplyComment ---

    @Test
    // Covers: BadRequest
    void createReplyComment_BadRequest() {
        assertThatThrownBy(() -> commentService.createReplyComment(10L, 1L, "", null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers: Comment Not Found
    void createReplyComment_CommentNotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.createReplyComment(10L, 1L, "Reply", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: User Not Found
    void createReplyComment_UserNotFound() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.createReplyComment(10L, 1L, "Reply", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Success
    void createReplyComment_Success() {
        User otherUser = new User();
        otherUser.setId(2L);
        comment.setUser(otherUser); // Trigger notification

        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(replyCommentRepository.save(any(ReplyComment.class))).thenAnswer(i -> {
            ReplyComment r = i.getArgument(0);
            r.setId(20L);
            return r;
        });

        ReplyCommentDTO result = commentService.createReplyComment(10L, 1L, "Reply", ReplyComment.Emotion.like, null);

        assertThat(result).isNotNull();
        verify(notificationService).createCommentNotification(isNull(), eq(2L), eq(1L), eq("testuser"), eq("COMMENT"),
                eq(10L));
    }

    // --- getReplyCommentsByCommentId ---

    @Test
    // Covers: Success
    void getReplyCommentsByCommentId_Success() {
        Page<ReplyComment> page = new PageImpl<>(Collections.singletonList(replyComment));
        when(replyCommentRepository.findByCommentId(eq(10L), any(Pageable.class))).thenReturn(page);

        Page<ReplyCommentDTO> result = commentService.getReplyCommentsByCommentId(10L, PageRequest.of(0, 10));
        assertThat(result).isNotEmpty();
    }

    // --- updateReplyComment ---

    @Test
    // Covers: BadRequest
    void updateReplyComment_BadRequest() {
        assertThatThrownBy(() -> commentService.updateReplyComment(user, authentication, 20L, "", null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    // Covers: Not Found
    void updateReplyComment_NotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.updateReplyComment(user, authentication, 20L, "New", null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Forbidden
    void updateReplyComment_Forbidden() {
        User otherUser = new User();
        otherUser.setId(2L);
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThatThrownBy(() -> commentService.updateReplyComment(otherUser, authentication, 20L, "New", null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    // Covers: Success
    void updateReplyComment_Success() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(replyCommentRepository.save(any(ReplyComment.class))).thenReturn(replyComment);

        ReplyCommentDTO result = commentService.updateReplyComment(user, authentication, 20L, "Updated", null, null);
        assertThat(result.getContent()).isEqualTo("Updated");
    }

    // --- deleteReplyComment ---

    @Test
    // Covers: Not Found
    void deleteReplyComment_NotFound() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.deleteReplyComment(user, authentication, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers: Forbidden
    void deleteReplyComment_Forbidden() {
        User otherUser = new User();
        otherUser.setId(2L);
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThatThrownBy(() -> commentService.deleteReplyComment(otherUser, authentication, 20L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    // Covers: Success
    void deleteReplyComment_Success() {
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        commentService.deleteReplyComment(user, authentication, 20L);
        verify(replyCommentRepository).delete(replyComment);
    }

    // --- isOwner ---

    @Test
    // Covers: Success (True)
    void isOwner_True() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsernameOrEmail("testuser")).thenReturn(user);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThat(commentService.isOwner(authentication, 10L)).isTrue();
    }

    @Test
    // Covers: Success (False)
    void isOwner_False() {
        User otherUser = new User();
        otherUser.setId(2L);

        when(authentication.getName()).thenReturn("other");
        when(userService.findByUsernameOrEmail("other")).thenReturn(otherUser);
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThat(commentService.isOwner(authentication, 10L)).isFalse();
    }

    // --- isReplyOwner ---

    @Test
    // Covers: Success (True)
    void isReplyOwner_True() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.findByUsernameOrEmail("testuser")).thenReturn(user);
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));

        assertThat(commentService.isReplyOwner(authentication, 20L)).isTrue();
    }

    // --- countCommentsByPostId ---

    @Test
    // Covers: Success
    void countCommentsByPostId_Success() {
        when(commentRepository.countByPostId(100L)).thenReturn(5L);
        assertThat(commentService.countCommentsByPostId(100L)).isEqualTo(5L);
    }

    // --- hasCommentPermission ---

    @Test
    // Covers: Not Authenticated
    void hasCommentPermission_NotAuth() {
        assertThat(commentService.hasCommentPermission(null, 10L)).isFalse();
    }

    @Test
    // Covers: Success (Owner)
    void hasCommentPermission_Owner() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("testuser"); // String principal
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThat(commentService.hasCommentPermission(authentication, 10L)).isTrue();
    }

    // --- hasReplyPermission ---

    @Test
    // Covers: Not Authenticated
    void hasReplyPermission_NotAuth() {
        assertThat(commentService.hasReplyPermission(null, 20L)).isFalse();
    }

    @Test
    // Covers: Success (Owner)
    void hasReplyPermission_Owner() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("testuser");
        when(replyCommentRepository.findById(20L)).thenReturn(Optional.of(replyComment));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThat(commentService.hasReplyPermission(authentication, 20L)).isTrue();
    }
}
