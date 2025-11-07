package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// Quan trọng: Import 'multipart'
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

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        mockMvc.perform(multipart("/posts/create")
                        .param("content", "Một bài viết mới toanh.")
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Một bài viết mới toanh."))
                .andExpect(jsonPath("$.privacy").value("open"))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WithOneImageFile_ShouldReturn200AndFileDetails() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "files",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image content".getBytes()
        );

        mockMvc.perform(multipart("/posts/create")
                        .file(imageFile)
                        .param("content", "Bài viết có ảnh")
                        .param("privacy", "friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Bài viết có ảnh"))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files.length()").value(1))
                .andExpect(jsonPath("$.files[0].fileName").value("test-image.jpg"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WithMultipleFiles_ShouldReturn200() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("files", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes());
        MockMultipartFile videoFile = new MockMultipartFile("files", "video.mp4", MediaType.APPLICATION_OCTET_STREAM_VALUE, "vid".getBytes());

        mockMvc.perform(multipart("/posts/create")
                        .file(imageFile)
                        .file(videoFile)
                        .param("content", "Bài viết có nhiều file")
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
        MockMultipartFile newFile = new MockMultipartFile("files", "new-file.jpg", MediaType.IMAGE_JPEG_VALUE, "new".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + postId)
                        .file(newFile)
                        .param("content", "Nội dung cũ, thêm file mới")
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[?(@.fileName == 'new-file.jpg')]").exists());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_DeleteFile_ShouldReturn200AndFileRemoved() throws Exception {
        MockMultipartFile fileToCreate = new MockMultipartFile("files", "file-to-delete.txt", MediaType.TEXT_PLAIN_VALUE, "deleteme".getBytes());
        MvcResult createResult = mockMvc.perform(multipart("/posts/create")
                        .file(fileToCreate)
                        .param("content", "Tạo để xóa file")
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Long createdPostId = JsonPath.parse(createResponse).read("$.id", Long.class);
        Integer createdFileId = JsonPath.parse(createResponse).read("$.files[0].id", Integer.class);

        // 2. Cập nhật post đó và xóa file
        mockMvc.perform(multipart(HttpMethod.PUT, "/posts/" + createdPostId)
                        .param("fileIdsToDelete", String.valueOf(createdFileId))
                        .param("content", "Đã xóa file")
                        .param("privacy", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Đã xóa file"))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files.length()").value(0));
    }


    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenPostNotFound_ShouldReturn404() throws Exception {
        Long nonExistentPostId = 9999L;
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
        mockMvc.perform(delete("/posts/" + postIdOfOtherUser))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deletePost_WhenPostNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        mockMvc.perform(delete("/posts/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostExists_ShouldReturn200AndPostDetails() throws Exception {
        Long postId = 10L;
        Long userId = 1L;

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
        Long ownerUserId = 1L;

        mockMvc.perform(get("/posts/" + privatePostId + "/user/" + ownerUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/posts/9999/user/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetPostsByUserId_WhenUserExists_ShouldReturn200AndListOfPosts() throws Exception {
        Long userId = 1L;
        mockMvc.perform(get("/posts/user/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetPostsByUserId_WhenUserNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/posts/user/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetAllPosts_DefaultPagination_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetAllPosts_WithPaginationParams_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/posts?page=0&size=1&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].id").value(10));
    }
}