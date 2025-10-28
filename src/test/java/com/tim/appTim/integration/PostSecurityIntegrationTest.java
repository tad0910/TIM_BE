package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles; // Import này
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;
import java.util.Map;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test") // Thêm dòng này
public class PostSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

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
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostExists_ShouldReturn200AndPostDetails() throws Exception {
        Long postId = 10L; // ID bài viết từ test-data.sql
        Long userId = 1L;  // ID của người dùng sở hữu bài viết

        mockMvc.perform(get("/posts/" + postId + "/user/" + userId)) // <-- URL khớp với Controller
                .andExpect(status().isOk()) // Mong đợi 200 OK
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value("Bài viết của owner")) // <-- Sửa thành $.content
                .andExpect(jsonPath("$.userId").value(userId));        // <-- Sửa thành $.userId
    }


    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetPostsByUserId_WhenUserExists_ShouldReturn200AndListOfPosts() throws Exception {
        Long userId = 1L; // Xem các bài viết của user ID 1 ("post_owner")

        mockMvc.perform(get("/posts/user/" + userId)) // <-- URL khớp với Controller
                .andExpect(status().isOk()) // Mong đợi 200 OK
                .andExpect(jsonPath("$").isArray()) // Kiểm tra kết quả là một mảng JSON
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(userId)); // Kiểm tra userId của bài viết đầu tiên

    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // Đăng nhập với user ID 1
    void testUpdatePost_WhenUserIsOwner_ShouldReturn200AndUpdatedPost() throws Exception {
        Long postId = 10L; // ID bài viết của user 1
        String updatedContent = "Nội dung đã được cập nhật.";
        String updatedPrivacy = "friends"; // Giá trị hợp lệ cho enum Post.Privacy

        mockMvc.perform(put("/posts/" + postId) // Gọi PUT API
                        .param("content", updatedContent)   // Gửi dữ liệu mới qua param
                        .param("privacy", updatedPrivacy)) // Gửi dữ liệu mới qua param
                .andExpect(status().isOk()) // Kiểm tra status 200 OK
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.content").value(updatedContent)) // Kiểm tra nội dung đã đổi
                .andExpect(jsonPath("$.privacy").value(updatedPrivacy)) // Kiểm tra privacy đã đổi
                .andExpect(jsonPath("$.userId").value(1L)); // Đảm bảo userId vẫn đúng
    }

    /**
     * Test case thất bại: Người dùng không phải chủ sở hữu bài viết.
     * Mong đợi: Status 403 Forbidden.
     */
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService") // Đăng nhập với user ID 2
    void testUpdatePost_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long postId = 10L; // ID bài viết của user 1
        String updatedContent = "Cố gắng cập nhật trái phép.";
        String updatedPrivacy = "only_me";

        mockMvc.perform(put("/posts/" + postId)
                        .param("content", updatedContent)
                        .param("privacy", updatedPrivacy))
                .andExpect(status().isForbidden()); // Kiểm tra status 403 Forbidden
    }

    /**
     * Test case thất bại: Bài viết không tồn tại.
     * Mong đợi: Status 404 Not Found.
     */
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // Đăng nhập với user ID 1
    void testUpdatePost_WhenPostNotFound_ShouldReturn404() throws Exception {
        Long nonExistentPostId = 9999L; // ID không tồn tại
        String updatedContent = "Cập nhật bài viết không tồn tại.";
        String updatedPrivacy = "open";
        mockMvc.perform(put("/posts/" + nonExistentPostId)
                        .param("content", updatedContent)
                        .param("privacy", updatedPrivacy))
                .andExpect(status().isNotFound()); // Kiểm tra status 404 Not Found
    }

    /**
     * Test case thất bại: Giá trị privacy không hợp lệ.
     * Mong đợi: Status 400 Bad Request.
     */
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // Đăng nhập với user ID 1
    void testUpdatePost_WhenInvalidPrivacyValue_ShouldReturn400() throws Exception {
        Long postId = 10L; // ID bài viết của user 1
        String updatedContent = "Nội dung hợp lệ.";
        String invalidPrivacy = "INVALID_VALUE"; // Giá trị không có trong enum Post.Privacy

        mockMvc.perform(put("/posts/" + postId)
                        .param("content", updatedContent)
                        .param("privacy", invalidPrivacy))
                .andExpect(status().isBadRequest()); // Kiểm tra status 400 Bad Request
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        String content = "Một bài viết mới toanh.";
        String privacy = "open";

        mockMvc.perform(post("/posts/create") // Đã sửa endpoint từ /posts thành /posts/create
                        .param("content", content)
                        .param("privacy", privacy))
                .andExpect(status().isOk()) // Hoặc isCreated() tùy vào Controller
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.privacy").value(privacy))
                .andExpect(jsonPath("$.userId").value(1L)); // ID của "post_owner"
    }

    @Test
    void testCreatePost_WhenUserIsAnonymous_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/posts/create") // Đã sửa endpoint từ /posts thành /posts/create
                        .param("content", "Nội dung ẩn danh")
                        .param("privacy", "open"))
                .andExpect(status().isUnauthorized()); // 401 Chưa đăng nhập
    }


    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostIsPrivateAndUserIsNotOwner_ShouldReturn403() throws Exception {
        Long privatePostId = 11L; // ID bài viết private của user 1
        Long ownerUserId = 1L;

        mockMvc.perform(get("/posts/" + privatePostId + "/user/" + ownerUserId))
                .andExpect(status().isForbidden()); // Mong đợi 403 Forbidden
    }
}