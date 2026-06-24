package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.File;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.UnprocessableException;
import com.tim.appTim.repository.FileRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentService commentService;
    @Mock
    private ReactionService reactionService;
    @Mock
    private com.tim.appTim.repository.CommentRepository commentRepository;
    @Mock
    private LinkPreviewService linkPreviewService;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private GamificationService gamificationService;
    @Mock
    private BehaviorLookupService behaviorLookupService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PostService postService;

    private User user;
    private User otherUser;
    private Post post;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setFirstName("User");
        user.setLastName("One");
        user.setProfileImage("profile.jpg");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("user2");

        post = new Post();
        post.setId(100L);
        post.setUser(user);
        post.setContent("Test Content");
        post.setPrivacy(Post.Privacy.open);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setFiles(new ArrayList<>());
    }

    // --- generateLinkPreviewAsync ---

    @Test
    // Covers branch: Post found, preview success
    void generateLinkPreviewAsync_Success() throws Exception {
        when(linkPreviewService.getLinkPreview("http://example.com"))
                .thenReturn(new LinkPreviewDTO("http://example.com", "Title", "Desc", "img.jpg", "example.com"));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        postService.generateLinkPreviewAsync(100L, "http://example.com");

        verify(postRepository).save(post);
        assertThat(post.getLinkTitle()).isEqualTo("Title");
    }

    @Test
    // Covers branch: Post not found
    void generateLinkPreviewAsync_Fail_PostNotFound() throws Exception {
        when(linkPreviewService.getLinkPreview("http://example.com"))
                .thenReturn(new LinkPreviewDTO("http://example.com", "Title", "Desc", "img.jpg", "example.com"));
        when(postRepository.findById(100L)).thenReturn(Optional.empty());

        postService.generateLinkPreviewAsync(100L, "http://example.com");

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    // Covers branch: URISyntaxException
    void generateLinkPreviewAsync_Fail_URISyntaxException() throws Exception {
        when(linkPreviewService.getLinkPreview(anyString())).thenThrow(new URISyntaxException("input", "reason"));
        postService.generateLinkPreviewAsync(100L, "invalid-url");
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    // Covers branch: IOException
    void generateLinkPreviewAsync_Fail_IOException() throws Exception {
        when(linkPreviewService.getLinkPreview(anyString())).thenThrow(new IOException("error"));
        postService.generateLinkPreviewAsync(100L, "http://example.com");
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    // Covers branch: Unexpected Exception
    void generateLinkPreviewAsync_Fail_Exception() throws Exception {
        when(linkPreviewService.getLinkPreview(anyString())).thenThrow(new RuntimeException("error"));
        postService.generateLinkPreviewAsync(100L, "http://example.com");
        verify(postRepository, never()).save(any(Post.class));
    }

    // --- createPostWithFiles ---

    @Test
    // Covers branch: Content empty AND Files empty
    void createPost_Fail_Empty() {
        assertThatThrownBy(() -> postService.createPostWithFiles(1L, "", Post.Privacy.open, null))
                .isInstanceOf(UnprocessableException.class)
                .hasMessageContaining("Bài viết phải có nội dung hoặc tệp đính kèm");
    }

    @Test
    // Covers branch: User not found
    void createPost_Fail_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.createPostWithFiles(1L, "Content", Post.Privacy.open, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Success with content, no files, no link
    void createPost_Success_ContentOnly() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> {
            Post p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        PostDTO dto = postService.createPostWithFiles(1L, "Content", Post.Privacy.open, null);
        assertThat(dto.getContent()).isEqualTo("Content");
    }

    @Test
    // Covers branch: Success with files (hasMedia = true)
    void createPost_Success_WithMedia() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> {
            Post p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        List<File> files = new ArrayList<>();
        File file = new File();
        file.setFileType(File.FileType.IMAGE);
        file.setFileUrl("http://img.com/1.jpg");
        files.add(file);

        PostDTO dto = postService.createPostWithFiles(1L, "Check http://google.com", Post.Privacy.open, files);
        // Should NOT extract link because hasMedia is true
        verify(linkPreviewService, never()).getLinkPreview(anyString());
        assertThat(dto.getFiles()).hasSize(1);
    }

    @Test
    // Covers branch: Success with link, no media -> extract link
    void createPost_Success_WithLink() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> {
            Post p = i.getArgument(0);
            p.setId(100L);
            return p;
        });
        // Mocking async call indirectly by ensuring no exception is thrown
        // Note: Async method is called on the proxy, but here we are calling method on
        // 'postService' which is @InjectMocks.
        // The async behavior depends on Spring context which is not present in unit
        // test.
        // So it executes synchronously. We need to mock linkPreviewService.

        // However, the method calls generateLinkPreviewAsync which is void.
        // We can verify it was called if we spy, but here we just ensure no error.

        // Actually, generateLinkPreviewAsync calls linkPreviewService.
        // We should mock that to verify flow.
        when(linkPreviewService.getLinkPreview(anyString()))
                .thenReturn(new LinkPreviewDTO("url", "t", "d", "i", "dom"));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post)); // For the async method to find the post

        postService.createPostWithFiles(1L, "Check http://google.com", Post.Privacy.open, null);

        // Verify link extraction logic
        // The save is called twice: once in create, once in generateLinkPreviewAsync
        verify(postRepository, atLeast(1)).save(any(Post.class));
    }

    @Test
    // Covers branch: First post award
    void createPost_Success_FirstPost() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenAnswer(i -> {
            Post p = i.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(postRepository.countByUserId(1L)).thenReturn(1L);
        when(behaviorLookupService.getIdByName("Đăng bài viết đầu tiên")).thenReturn(10);

        postService.createPostWithFiles(1L, "Content", Post.Privacy.open, null);
        verify(gamificationService).awardPoints(1L, 10);
    }

    // --- getAllPosts ---

    @Test
    // Covers branch: Success
    void getAllPosts_Success() {
        Page<Post> page = new PageImpl<>(Collections.singletonList(post));
        when(postRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<PostDTO> result = postService.getAllPosts(PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    // --- getPostsByUserId ---

    @Test
    // Covers branch: User not found
    void getPostsByUserId_Fail_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getPostsByUserId(99L, Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Success
    void getPostsByUserId_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Page<Post> page = new PageImpl<>(Collections.singletonList(post));
        when(postRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<PostDTO> result = postService.getPostsByUserId(1L, PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    // --- updatePostWithFiles ---

    @Test
    // Covers branch: Post not found
    void updatePost_Fail_NotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(
                () -> postService.updatePostWithFiles(user, authentication, 99L, "c", Post.Privacy.open, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Forbidden (Not owner, not admin)
    void updatePost_Fail_Forbidden() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThatThrownBy(() -> postService.updatePostWithFiles(otherUser, authentication, 100L, "c",
                Post.Privacy.open, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    // Covers branch: Success (Owner)
    void updatePost_Success_Owner() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postService.updatePostWithFiles(user, authentication, 100L, "New Content", Post.Privacy.open, null, null);

        verify(postRepository).save(post);
        assertThat(post.getContent()).isEqualTo("New Content");
    }

    @Test
    // Covers branch: Success (Admin)
    void updatePost_Success_Admin() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("post:update_all"));
        doReturn(authorities).when(authentication).getAuthorities();
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postService.updatePostWithFiles(otherUser, authentication, 100L, "Admin Edit", Post.Privacy.open, null, null);

        verify(postRepository).save(post);
    }

    @Test
    // Covers branch: Delete files
    void updatePost_Success_DeleteFiles() {
        File f1 = new File();
        f1.setId(10);
        post.addFile(f1);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postService.updatePostWithFiles(user, authentication, 100L, "c", Post.Privacy.open, null,
                Collections.singletonList(10));

        assertThat(post.getFiles()).isEmpty();
    }

    @Test
    // Covers branch: Add new files
    void updatePost_Success_AddFiles() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(postRepository.save(any(Post.class))).thenReturn(post);

        List<File> newFiles = new ArrayList<>();
        File newFile = new File();
        newFile.setFileType(File.FileType.IMAGE);
        newFiles.add(newFile);

        postService.updatePostWithFiles(user, authentication, 100L, "c", Post.Privacy.open, newFiles, null);

        assertThat(post.getFiles()).hasSize(1);
        verify(fileRepository).save(any(File.class));
    }

    @Test
    // Covers branch: Update link (new URL)
    void updatePost_Success_UpdateLink() throws Exception {
        post.setLinkUrl("http://old.com");
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // For async call
        when(linkPreviewService.getLinkPreview(anyString()))
                .thenReturn(new LinkPreviewDTO("new", "t", "d", "i", "dom"));

        postService.updatePostWithFiles(user, authentication, 100L, "Check http://new.com", Post.Privacy.open, null,
                null);

        assertThat(post.getLinkUrl()).isEqualTo("http://new.com");
    }

    @Test
    // Covers branch: Remove link (no URL in new content)
    void updatePost_Success_RemoveLink() {
        post.setLinkUrl("http://old.com");
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postService.updatePostWithFiles(user, authentication, 100L, "No link here", Post.Privacy.open, null, null);

        assertThat(post.getLinkUrl()).isNull();
    }

    // --- deletePost ---

    @Test
    // Covers branch: Post not found
    void deletePost_Fail_NotFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.deletePost(user, authentication, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Forbidden
    void deletePost_Fail_Forbidden() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        assertThatThrownBy(() -> postService.deletePost(otherUser, authentication, 100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    // Covers branch: Success
    void deletePost_Success() {
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();
        postService.deletePost(user, authentication, 100L);
        verify(postRepository).delete(post);
    }

    // --- isOwner ---

    @Test
    // Covers branch: User not found
    void isOwner_Fail_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThat(postService.isOwner("unknown", 100L)).isFalse();
    }

    @Test
    // Covers branch: Post not found
    void isOwner_Fail_PostNotFound() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(postRepository.findById(100L)).thenReturn(Optional.empty());
        assertThat(postService.isOwner("user1", 100L)).isFalse();
    }

    @Test
    // Covers branch: Success
    void isOwner_Success() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        assertThat(postService.isOwner("user1", 100L)).isTrue();
    }

    // --- getPostByIdForUser ---

    @Test
    // Covers branch: User not found
    void getPostByIdForUser_Fail_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getPostByIdForUser(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Post not found
    void getPostByIdForUser_Fail_PostNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getPostByIdForUser(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    // Covers branch: Success (Owner)
    void getPostByIdForUser_Success_Owner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        PostDTO dto = postService.getPostByIdForUser(1L, 100L);
        assertThat(dto.getId()).isEqualTo(100L);
    }

    @Test
    // Covers branch: Privacy Only Me (Not Owner)
    void getPostByIdForUser_Fail_OnlyMe() {
        post.setPrivacy(Post.Privacy.only_me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPostByIdForUser(2L, 100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    // Covers branch: Privacy Open (Not Owner)
    void getPostByIdForUser_Success_Open() {
        post.setPrivacy(Post.Privacy.open);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        PostDTO dto = postService.getPostByIdForUser(2L, 100L);
        assertThat(dto.getId()).isEqualTo(100L);
    }

    // --- isPostOwner (Authentication) ---

    @Test
    // Covers branch: Not authenticated
    void isPostOwner_Fail_NotAuth() {
        assertThat(postService.isPostOwner(null, 100L)).isFalse();
    }

    @Test
    // Covers branch: Success (UserDetails)
    void isPostOwner_Success_UserDetails() {
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn("user1");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(ud);

        when(postRepository.findById(100L)).thenReturn(Optional.of(post));

        assertThat(postService.isPostOwner(authentication, 100L)).isTrue();
    }
}

