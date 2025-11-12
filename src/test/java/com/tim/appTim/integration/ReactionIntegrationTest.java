package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.PostService;
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

    @MockBean
    private PostService postService;

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

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactionsByPostId_ShouldReturnReactionPage() throws Exception {
        Long postId = 10L;
        mockMvc.perform(get("/reactions/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].emotionType").value("like"))
                .andExpect(jsonPath("$.content[0].userId").value(2L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testCountReactionsByType_ShouldReturnCount() throws Exception {
        Long postId = 10L;
        String emotion = "like";
        mockMvc.perform(get("/reactions/posts/" + postId + "/count/" + emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactionsByCommentId_ShouldReturnReactionPage() throws Exception {
        Long commentId = 20L;
        mockMvc.perform(get("/reactions/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCountCommentReactionsByType_ShouldReturnCorrectCount() throws Exception {
        Long commentId = 20L;
        String emotion = "love";

        mockMvc.perform(post("/reactions/comments/" + commentId)
                        .param("emotionType", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value(emotion));

        mockMvc.perform(get("/reactions/comments/" + commentId + "/count/" + emotion)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("another_user").roles("USER"))) // Đổi user để test
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1L));

        mockMvc.perform(get("/reactions/comments/" + commentId + "/count/" + "like")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("another_user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactionsByReplyId_ShouldReturnReactionPage() throws Exception {
        Long replyId = 30L;
        mockMvc.perform(get("/reactions/replies/" + replyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].emotionType").value("haha"))
                .andExpect(jsonPath("$.content[0].userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testCountReplyReactionsByType_ShouldReturnCount() throws Exception {
        Long replyId = 30L;
        String emotion = "haha";
        mockMvc.perform(get("/reactions/replies/" + replyId + "/count/" + emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1L));
    }

    @Test
    void testPostReaction_WhenUserIsNotAuthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/reactions/posts/10")
                        .param("emotionType", "like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testDeleteCommentReaction_WhenUserLacksAuthority_ShouldReturn403() throws Exception {
        Long commentId = 20L;
        mockMvc.perform(delete("/reactions/comments/" + commentId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testDeleteReplyReaction_WhenUserLacksAuthority_ShouldReturn403() throws Exception {
        Long replyId = 30L;
        mockMvc.perform(delete("/reactions/replies/" + replyId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testPostReaction_WhenEmotionTypeIsInvalid_ShouldReturn400() throws Exception {
        Long postId = 10L;
        mockMvc.perform(post("/reactions/posts/" + postId)
                        .param("emotionType", "invalid_emotion_string"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testCountReactions_WhenEmotionTypeIsInvalid_ShouldReturn400() throws Exception {
        Long postId = 10L;
        mockMvc.perform(get("/reactions/posts/" + postId + "/count/invalid_string"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testPostReaction_WhenPostNotFound_ShouldReturn404() throws Exception {
        Long nonExistentPostId = 9999L;
        mockMvc.perform(post("/reactions/posts/" + nonExistentPostId)
                        .param("emotionType", "like"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetReactions_WhenCommentNotFound_ShouldReturn404() throws Exception {
        Long nonExistentCommentId = 9999L;
        mockMvc.perform(get("/reactions/comments/" + nonExistentCommentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testAddReactionToPost_WhenReactionAlreadyExists_ShouldUpdateEmotion() throws Exception {
        Long postId = 10L;
        mockMvc.perform(post("/reactions/posts/" + postId)
                        .param("emotionType", "like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").value("like"))
                .andExpect(jsonPath("$.userId").value(2L));

        mockMvc.perform(post("/reactions/posts/" + postId)
                        .param("emotionType", "love"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotionType").   value("love"))
                .andExpect(jsonPath("$.userId").value(2L));
    }








}