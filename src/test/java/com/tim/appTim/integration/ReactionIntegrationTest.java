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

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ReactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testAddReactionToPost_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        Long postId = 10L;
        String emotion = "like";

        mockMvc.perform(post("/reactions/posts/" + postId)
                        // SỬA: Đổi tên parameter thành "emotionType"
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
                        // SỬA: Đổi tên parameter thành "emotionType"
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
                        // SỬA: Đổi tên parameter thành "emotionType"
                        .param("emotionType", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value(emotion))
                .andExpect(jsonPath("$.userId").value(2L));
    }

    // Giả sử API là DELETE /reactions/posts/{postId} (chỉ xóa reaction của user hiện tại)
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService") // User 2 là owner của reaction
    void testRemoveReactionFromPost_WhenUserIsOwner_ShouldReturn204() throws Exception {
        Long postId = 10L; // ID của post mà user 2 đã react

        mockMvc.perform(delete("/reactions/posts/" + postId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // User 1 KHÔNG phải owner reaction 40L
    void testRemoveReactionFromPost_WhenUserIsNotOwner_ShouldReturn200_OK() throws Exception {
        Long postId = 10L; // ID của post

        // Gọi API xóa reaction của user 1 khỏi post 10.
        // Vì user 1 chưa react post 10, service sẽ không làm gì và trả về 200 OK.
        mockMvc.perform(delete("/reactions/posts/" + postId))
                .andExpect(status().isNoContent());
    }


}