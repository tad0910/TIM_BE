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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;


@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test") // Thêm dòng này
public class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createComment_WhenUserIsAuthenticated_ShouldReturnCorrectStatus() throws Exception { // Đổi tên test nếu cần

        Long postId = 10L;
        String noiDungBinhLuan = "Đây là một bình luận test tuyệt vời!";

        // Test này sẽ gọi URL: /comments/posts/10?content=...
        mockMvc.perform(post("/comments/posts/" + postId) // <-- Sửa URL
                                .param("content", noiDungBinhLuan) // <-- Gửi như RequestParam
                        // .param("emotion", "LIKE") // Thêm nếu cần test emotion
                        // .param("fileId", "123") // Thêm nếu cần test fileId
                ) // Không cần contentType hay content() nữa
                .andExpect(status().isOk()) // <-- Controller hiện đang trả về OK (200), không phải Created (201)
                .andExpect(jsonPath("$.content").value(noiDungBinhLuan))
                .andExpect(jsonPath("$.userId").value(2L)); // ID của "another_user"

    }

    @Test
    void createComment_WhenUserIsAnonymous_ShouldReturn401() throws Exception {
        // --- Chuẩn bị dữ liệu ---
        // Long userId = 2L; // Không cần userId trong URL nữa
        Long postId = 10L;
        String noiDungBinhLuan = "Bình luận này sẽ thất bại";

        // --- Gọi API và Kiểm tra ---
        // Lần này chúng ta KHÔNG dùng @WithUserDetails
        mockMvc.perform(post("/comments/posts/" + postId) // <-- Sửa URL
                        .param("content", noiDungBinhLuan)) // <-- Gửi như RequestParam thay vì JSON Body

                // --- Kiểm tra dữ liệu trả về ---
                // Kỳ vọng lỗi 401 Unauthorized (Chưa đăng nhập)
                .andExpect(status().isUnauthorized());
    }

    // --- TEST CASES CHO SỬA COMMENT ---

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService") // User 2 là owner của comment 20
    void testUpdateComment_WhenUserIsOwner_ShouldReturn200AndUpdatedComment() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Nội dung comment đã được cập nhật.";

        mockMvc.perform(put("/comments/" + commentId)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.userId").value(2L)); // Vẫn là user 2
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // User 1 không phải owner
    void testUpdateComment_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Cố gắng sửa comment người khác.";

        mockMvc.perform(put("/comments/" + commentId)
                        .param("content", updatedContent))
                .andExpect(status().isForbidden()); // Mong đợi 403
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService") // Admin có quyền update_all
    void testUpdateComment_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Admin cập nhật comment.";

        mockMvc.perform(put("/comments/" + commentId)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(updatedContent));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testUpdateComment_WhenCommentNotFound_ShouldReturn404() throws Exception {
        Long nonExistentCommentId = 999L;
        String updatedContent = "Sửa comment không tồn tại.";

        mockMvc.perform(put("/comments/" + nonExistentCommentId)
                        .param("content", updatedContent))
                .andExpect(status().isNotFound()); // Mong đợi 404
    }

    // --- TEST CASES CHO XÓA COMMENT ---

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService") // User 2 là owner
    void testDeleteComment_WhenUserIsOwner_ShouldReturn200() throws Exception {
        Long commentId = 20L;

        mockMvc.perform(delete("/comments/" + commentId))
                .andExpect(status().isOk());
        // Có thể thêm kiểm tra xem comment có thực sự bị xóa không bằng cách gọi API GET sau đó
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // User 1 không phải owner
    void testDeleteComment_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long commentId = 20L;

        mockMvc.perform(delete("/comments/" + commentId))
                .andExpect(status().isForbidden()); // Mong đợi 403
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService") // Admin có quyền delete_all
    void testDeleteComment_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long commentId = 20L;

        mockMvc.perform(delete("/comments/" + commentId))
                .andExpect(status().isOk());
    }
}