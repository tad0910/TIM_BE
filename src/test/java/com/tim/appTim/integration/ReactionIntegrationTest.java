package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ReactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    private final String BASE_URL = "/schedules";

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testAddReactionToPost_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        Long postId = 10L;
        String emotion = "like";

        mockMvc.perform(post("/reactions/posts/" + postId)
                        .param("emotionType", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value(emotion))
                .andExpect(jsonPath("$.userId").value(2L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testAddReactionToComment_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        Long commentId = 20L;
        String emotion = "love";

        mockMvc.perform(post("/reactions/comments/" + commentId)
                        .param("emotionType", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value(emotion))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testAddReactionToReply_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        Long replyId = 30L;
        String emotion = "haha";

        mockMvc.perform(post("/reactions/replies/" + replyId)
                        .param("emotionType", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value(emotion))
                .andExpect(jsonPath("$.userId").value(2L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testRemoveReactionFromPost_WhenUserIsOwner_ShouldReturn204() throws Exception {
        Long postId = 10L;

        mockMvc.perform(delete("/reactions/posts/" + postId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testRemoveReactionFromPost_WhenUserIsNotOwner_ShouldReturn200_OK() throws Exception {
        Long postId = 10L;
        mockMvc.perform(delete("/reactions/posts/" + postId))
                .andExpect(status().isNoContent());
    }

    // Test lấy tất cả reaction của một post
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactionsByPostId_ShouldReturnReactionList() throws Exception {
        Long postId = 10L;
        // Dữ liệu: User 2 đã reaction "like" vào post 10
        mockMvc.perform(get("/reactions/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].emotionType").value("like"))
                .andExpect(jsonPath("$[0].userId").value(2L)); // Reaction của user 2
    }

    // Test đếm reaction theo loại của một post
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testCountReactionsByType_ShouldReturnCount() throws Exception {
        Long postId = 10L;
        String emotion = "like";
        // Giả sử post 10L có 1 reaction "like"
        mockMvc.perform(get("/reactions/posts/" + postId + "/count/" + emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1L)); // Giả sử kết quả đếm là 1
    }

    // Test lấy reaction của comment
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactionsByCommentId_ShouldReturnReactionList() throws Exception {
        Long commentId = 20L;
        mockMvc.perform(get("/reactions/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))) // Giả sử có 1 reaction
                .andExpect(jsonPath("$[0].userId").value(1L)); // Giả sử user 1 đã reaction
    }

    // Test đếm reaction của comment
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // Dùng user sẽ tạo reaction
    void testCountCommentReactionsByType_ShouldReturnCorrectCount() throws Exception {
        Long commentId = 20L;
        String emotion = "love";

        // --- Bước 1: THÊM reaction "love" vào comment 20 ---
        // Chúng ta dùng "post_owner" (ID 1) để thêm
        mockMvc.perform(post("/reactions/comments/" + commentId)
                        .param("emotionType", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value(emotion));

        // --- Bước 2: ĐẾM số lượng reaction "love" ---
        // Bây giờ chúng ta gọi API đếm, có thể dùng bất kỳ user nào
        // (Ở đây tôi dùng "another_user" để cho thấy việc đếm không cần là chủ reaction)
        mockMvc.perform(get("/reactions/comments/" + commentId + "/count/" + emotion)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("another_user").roles("USER"))) // Đổi user để test
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1L)); // Kỳ vọng là 1

        // --- Bước 3 (Cẩn thận hơn): Đếm một loại reaction khác ---
        // Đảm bảo rằng nó không đếm nhầm
        mockMvc.perform(get("/reactions/comments/" + commentId + "/count/" + "like")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("another_user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0L)); // Kỳ vọng là 0
    }

    // Test lấy reaction của reply
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactionsByReplyId_ShouldReturnReactionList() throws Exception {
        Long replyId = 30L;
        // Dữ liệu: User 1 đã reaction "haha" vào reply 30
        mockMvc.perform(get("/reactions/replies/" + replyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].emotionType").value("haha"))
                .andExpect(jsonPath("$[0].userId").value(1L)); // Reaction của user 1
    }

    // Test đếm reaction của reply
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testCountReplyReactionsByType_ShouldReturnCount() throws Exception {
        Long replyId = 30L;
        String emotion = "haha";
        mockMvc.perform(get("/reactions/replies/" + replyId + "/count/" + emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1L)); // Giả sử đếm được 1
    }




    // Test 401 Unauthorized (Chưa xác thực)
    @Test
    void testPostReaction_WhenUserIsNotAuthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/reactions/posts/10")
                        .param("emotionType", "like"))
                .andExpect(status().isUnauthorized()); // 401
    }

    // Test 403 Forbidden (Không có quyền)
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    // 'another_user' (ID 2) không có quyền 'reaction:delete'
    void testDeleteCommentReaction_WhenUserLacksAuthority_ShouldReturn403() throws Exception {
        Long commentId = 20L;
        mockMvc.perform(delete("/reactions/comments/" + commentId))
                .andExpect(status().isForbidden()); // 403
    }

    // Test 403 Forbidden (Không có quyền) cho Replies
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    // 'another_user' (ID 2) không có quyền 'reaction:delete'
    void testDeleteReplyReaction_WhenUserLacksAuthority_ShouldReturn403() throws Exception {
        Long replyId = 30L;
        mockMvc.perform(delete("/reactions/replies/" + replyId))
                .andExpect(status().isForbidden()); // 403
    }

    // Test 400 Bad Request (Tham số không hợp lệ)
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testPostReaction_WhenEmotionTypeIsInvalid_ShouldReturn400() throws Exception {
        Long postId = 10L;
        mockMvc.perform(post("/reactions/posts/" + postId)
                        .param("emotionType", "invalid_emotion_string")) // Một chuỗi không hợp lệ
                .andExpect(status().isBadRequest()); // 400
    }

    // Test 400 Bad Request cho endpoint đếm
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testCountReactions_WhenEmotionTypeIsInvalid_ShouldReturn400() throws Exception {
        Long postId = 10L;
        mockMvc.perform(get("/reactions/posts/" + postId + "/count/invalid_string"))
                .andExpect(status().isBadRequest()); // 400
    }

    // Test 404 Not Found (Tài nguyên không tồn tại)
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testPostReaction_WhenPostNotFound_ShouldReturn404() throws Exception {
        Long nonExistentPostId = 9999L; // Một ID không có trong test-data.sql
        mockMvc.perform(post("/reactions/posts/" + nonExistentPostId)
                        .param("emotionType", "like"))
                .andExpect(status().isNotFound()); // 404 (Giả sử Service ném ra NotFoundException)
    }

    // Test 404 Not Found (Tài nguyên không tồn tại)
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactions_WhenCommentNotFound_ShouldReturn404() throws Exception {
        Long nonExistentCommentId = 9999L;
        mockMvc.perform(get("/reactions/comments/" + nonExistentCommentId))
                .andExpect(status().isNotFound()); // 404 (Giả sử Service ném ra NotFoundException)
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testAddReactionToPost_WhenReactionAlreadyExists_ShouldUpdateEmotion() throws Exception {
        Long postId = 10L;

        // Bước 1: User "another_user" (ID 2) thả "like"
        mockMvc.perform(post("/reactions/posts/" + postId)
                        .param("emotionType", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value("like"))
                .andExpect(jsonPath("$.userId").value(2L));

        // Bước 2: User "another_user" (ID 2) đổi thành "love"
        mockMvc.perform(post("/reactions/posts/" + postId)
                        .param("emotionType", "love"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value("love")) // Kiểm tra đã được cập nhật
                .andExpect(jsonPath("$.userId").value(2L));
    }








}