package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.FileDTO;
import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.entity.Post;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.service.FileUploadService;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class PostSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @MockBean
    private FileUploadService fileUploadService;

    @SpyBean
    private PostService postService;

    @BeforeEach
    void setUp() {
        // Setup default mock for FileUploadService to return a URL for any file upload
        doReturn("url/uploaded-file.jpg").when(fileUploadService).uploadFile(any());
    }

    private PostDTO createMockPostDTO(Long id, Long userId, String content, String privacy, List<FileDTO> files) {
        return new PostDTO(
                id, userId, content, privacy, LocalDateTime.now(), LocalDateTime.now(),
                0, 0, new ArrayList<>(), new ArrayList<>(), files,
                "profile.jpg", "username", "Display Name", null
        );
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        String content = "Một bài viết mới toanh.";
        PostDTO mockPost = createMockPostDTO(1L, 1L, content, "open", new ArrayList<>());
        doReturn(mockPost).when(postService).createPostWithFiles(eq(1L), eq(content), eq(Post.Privacy.open), any(List.class));

        mockMvc.perform(multipart("/posts/create")
                        .param("content", content)
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.privacy").value("open"))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WithOneImageFile_ShouldReturn200AndFileDetails() throws Exception {
        String content = "Bài viết có ảnh";
        FileDTO mockFile = new FileDTO(1, "url/test-image.jpg", "IMAGE", "test-image.jpg", 123L);
        PostDTO mockPost = createMockPostDTO(2L, 1L, content, "friends", List.of(mockFile));

        doReturn(mockPost).when(postService).createPostWithFiles(eq(1L), eq(content), eq(Post.Privacy.friends), any(List.class));
        doReturn("url/test-image.jpg").when(fileUploadService).uploadFile(any());

        MockMultipartFile imageFile = new MockMultipartFile(
                "files",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );

        mockMvc.perform(multipart("/posts/create")
                        .file(imageFile)
                        .param("content", content)
                        .param("privacy", "friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].fileName").value("test-image.jpg"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WithMultipleFiles_ShouldReturn200() throws Exception {
        String content = "Bài viết có nhiều file";
        FileDTO mockFile1 = new FileDTO(1, "url/image.jpg", "IMAGE", "image.jpg", 123L);
        FileDTO mockFile2 = new FileDTO(2, "url/video.mp4", "VIDEO", "video.mp4", 456L);
        PostDTO mockPost = createMockPostDTO(3L, 1L, content, "open", List.of(mockFile1, mockFile2));

        doReturn(mockPost).when(postService).createPostWithFiles(eq(1L), eq(content), eq(Post.Privacy.open), any(List.class));
        doReturn("url/uploaded-file.jpg").when(fileUploadService).uploadFile(any());

        MockMultipartFile imageFile = new MockMultipartFile("files", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes());
        MockMultipartFile videoFile = new MockMultipartFile("files", "video.mp4", MediaType.APPLICATION_OCTET_STREAM_VALUE, "vid".getBytes());

        mockMvc.perform(multipart("/posts/create")
                        .file(imageFile)
                        .file(videoFile)
                        .param("content", content)
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files.length()").value(2));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WhenInvalidPrivacyValue_ShouldReturn400() throws Exception {
        mockMvc.perform(multipart("/posts/create")
                        .param("content", "Nội dung hợp lệ")
                        .param("privacy", "INVALID_VALUE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testCreatePost_WhenUserHasNoCreatePermission_ShouldReturn403() throws Exception {
        mockMvc.perform(multipart("/posts/create")
                        .param("content", "Admin thử tạo post")
                        .param("privacy", "open"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreatePost_WhenUserIsAnonymous_ShouldReturn401() throws Exception {
        mockMvc.perform(multipart("/posts/create")
                        .param("content", "Nội dung ẩn danh")
                        .param("privacy", "open"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenUserIsOwner_ShouldReturn200AndUpdatedPost() throws Exception {
        Long postId = 10L;
        String updatedContent = "Nội dung đã được cập nhật.";
        String updatedPrivacy = "friends";
        PostDTO mockPost = createMockPostDTO(postId, 1L, updatedContent, updatedPrivacy, new ArrayList<>());

        doReturn(mockPost).when(postService).updatePostWithFiles(any(), any(), eq(postId), eq(updatedContent), eq(Post.Privacy.friends), any(), any());

        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + postId)
                        .param("content", updatedContent)
                        .param("privacy", updatedPrivacy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.privacy").value(updatedPrivacy))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long postId = 10L;
        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + postId)
                        .param("content", "Cố gắng cập nhật trái phép.")
                        .param("privacy", "only_me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long postId = 10L;
        String updatedContent = "Admin cập nhật bài viết.";
        PostDTO mockPost = createMockPostDTO(postId, 1L, updatedContent, "open", new ArrayList<>());

        doReturn(mockPost).when(postService).updatePostWithFiles(any(), any(), eq(postId), eq(updatedContent), eq(Post.Privacy.open), any(), any());

        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + postId)
                        .param("content", updatedContent)
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(updatedContent));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_AddFile_ShouldReturn200AndNewFile() throws Exception {
        Long postId = 10L;
        String content = "Nội dung cũ, thêm file mới";
        FileDTO newFileDTO = new FileDTO(2, "url/new-file.jpg", "IMAGE", "new-file.jpg", 123L);
        PostDTO mockPost = createMockPostDTO(postId, 1L, content, "open", List.of(newFileDTO));

        doReturn(mockPost).when(postService).updatePostWithFiles(any(), any(), eq(postId), eq(content), eq(Post.Privacy.open), any(), any());

        MockMultipartFile newFile = new MockMultipartFile("files", "new-file.jpg", MediaType.IMAGE_JPEG_VALUE, "new".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + postId)
                        .file(newFile)
                        .param("content", content)
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[?(@.fileName == 'new-file.jpg')]").exists());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenPostNotFound_ShouldReturn404() throws Exception {
        Long nonExistentPostId = 9999L;

        doThrow(new ResourceNotFoundException("Post not found with id: " + nonExistentPostId))
                .when(postService).updatePostWithFiles(any(), any(), eq(nonExistentPostId), any(), any(), any(), any());

        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + nonExistentPostId)
                        .param("content", "Cập nhật bài viết không tồn tại.")
                        .param("privacy", "open"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenInvalidPrivacyValue_ShouldReturn400() throws Exception {
        Long postId = 10L;
        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + postId)
                        .param("content", "Nội dung hợp lệ.")
                        .param("privacy", "INVALID_VALUE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deletePost_WhenUserIsOwner_ShouldReturn200() throws Exception {
        Long postIdOwnedByUser = 10L;
        doNothing().when(postService).deletePost(any(), any(), eq(postIdOwnedByUser));

        mockMvc.perform(delete("/posts/" + postIdOwnedByUser))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void deletePost_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long postIdNotOwnedByUser = 10L;
        mockMvc.perform(delete("/posts/" + postIdNotOwnedByUser))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deletePost_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long postIdOfOtherUser = 12L;
        doNothing().when(postService).deletePost(any(), any(), eq(postIdOfOtherUser));

        mockMvc.perform(delete("/posts/" + postIdOfOtherUser))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostExists_ShouldReturn200AndPostDetails() throws Exception {
        Long postId = 10L;
        Long userId = 1L;
        PostDTO mockPost = createMockPostDTO(postId, userId, "Bài viết của owner", "open", new ArrayList<>());
        doReturn(mockPost).when(postService).getPostByIdForUser(eq(userId), eq(postId));

        mockMvc.perform(get("/posts/" + postId + "/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value("Bài viết của owner"))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostIsPrivateAndUserIsNotOwner_ShouldReturn403() throws Exception {
        Long privatePostId = 11L;
        Long currentUserId = 2L;

        doThrow(new ForbiddenException("User does not have permission to access this post"))
                .when(postService).getPostByIdForUser(eq(currentUserId), eq(privatePostId));

        mockMvc.perform(get("/posts/" + privatePostId + "/user/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Post not found"))
                .when(postService).getPostByIdForUser(eq(1L), eq(9999L));

        mockMvc.perform(get("/posts/9999/user/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetPostsByUserId_WhenUserExists_ShouldReturn200AndListOfPosts() throws Exception {
        Long userId = 1L;
        PostDTO mockPost = createMockPostDTO(10L, userId, "Bài viết của owner", "open", new ArrayList<>());
        Page<PostDTO> mockPage = new PageImpl<>(List.of(mockPost));

        doReturn(mockPage).when(postService).getPostsByUserId(eq(userId), any(Pageable.class));

        mockMvc.perform(get("/posts/user/" + userId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].userId").value(userId));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetPostsByUserId_WhenUserNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 9999"))
                .when(postService).getPostsByUserId(eq(9999L), any(Pageable.class));

        mockMvc.perform(get("/posts/user/9999")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetAllPosts_DefaultPagination_ShouldReturn200() throws Exception {
        PostDTO mockPost1 = createMockPostDTO(10L, 1L, "Post 1", "open", new ArrayList<>());
        PostDTO mockPost2 = createMockPostDTO(11L, 1L, "Post 2", "only_me", new ArrayList<>());
        PostDTO mockPost3 = createMockPostDTO(12L, 2L, "Post 3", "open", new ArrayList<>());
        Page<PostDTO> mockPage = new PageImpl<>(List.of(mockPost1, mockPost2, mockPost3), PageRequest.of(0, 20), 3);

        doReturn(mockPage).when(postService).getAllPosts(any(Pageable.class));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetAllPosts_WithPaginationParams_ShouldReturn200() throws Exception {
        PostDTO mockPost1 = createMockPostDTO(10L, 1L, "Post 1", "open", new ArrayList<>());
        Page<PostDTO> mockPage = new PageImpl<>(List.of(mockPost1), PageRequest.of(0, 1), 3);

        doReturn(mockPage).when(postService).getAllPosts(any(Pageable.class));

        mockMvc.perform(get("/posts?page=0&size=1&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].id").value(10));
    }
}
