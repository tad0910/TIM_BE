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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void testUpdatePost_WhenUserIsOwner_ShouldReturn200AndUpdatedPost() throws Exception {
        Long postId = 10L;
        String updatedContent = "Nội dung đã được cập nhật.";
        String updatedPrivacy = "friends";

        mockMvc.perform(put("/posts/" + postId)
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
        String updatedContent = "Cố gắng cập nhật trái phép.";
        String updatedPrivacy = "only_me";

        mockMvc.perform(put("/posts/" + postId)
                        .param("content", updatedContent)
                        .param("privacy", updatedPrivacy))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenPostNotFound_ShouldReturn404() throws Exception {
        Long nonExistentPostId = 9999L;
        String updatedContent = "Cập nhật bài viết không tồn tại.";
        String updatedPrivacy = "open";
        mockMvc.perform(put("/posts/" + nonExistentPostId)
                        .param("content", updatedContent)
                        .param("privacy", updatedPrivacy))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdatePost_WhenInvalidPrivacyValue_ShouldReturn400() throws Exception {
        Long postId = 10L;
        String updatedContent = "Nội dung hợp lệ.";
        String invalidPrivacy = "INVALID_VALUE";

        mockMvc.perform(put("/posts/" + postId)
                        .param("content", updatedContent)
                        .param("privacy", invalidPrivacy))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreatePost_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        String content = "Một bài viết mới toanh.";
        String privacy = "open";

        mockMvc.perform(post("/posts/create")
                        .param("content", content)
                        .param("privacy", privacy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.privacy").value(privacy))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void testCreatePost_WhenUserIsAnonymous_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/posts/create")
                        .param("content", "Nội dung ẩn danh")
                        .param("privacy", "open"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetPostById_WhenPostIsPrivateAndUserIsNotOwner_ShouldReturn403() throws Exception {
        Long privatePostId = 11L;
        Long ownerUserId = 1L;

        mockMvc.perform(get("/posts/" + privatePostId + "/user/" + ownerUserId))
                .andExpect(status().isForbidden());
    }
}
