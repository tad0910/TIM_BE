package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;


@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @MockBean
    private PostService postService;

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetNotifications_WhenUserIsAuthenticated_ShouldReturn200AndList() throws Exception {
        Long currentUserId = 1L;

        mockMvc.perform(get("/notifications/user/" + currentUserId)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(50L))
                .andExpect(jsonPath("$.content[1].id").value(51L))
                .andExpect(jsonPath("$.content[0].isRead").value(false));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetUnreadNotificationCount_ShouldReturn200AndCount() throws Exception {
        Long currentUserId = 1L;

        mockMvc.perform(get("/notifications/user/" + currentUserId + "/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testMarkNotificationAsRead_WhenUserIsOwner_ShouldReturn200() throws Exception {
        Long notificationId = 50L;

        mockMvc.perform(put("/notifications/" + notificationId + "/mark-read"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification marked as read"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testMarkNotificationAsRead_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long notificationId = 50L;

        mockMvc.perform(put("/notifications/" + notificationId + "/mark-read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testMarkNotificationAsRead_WhenNotificationNotFound_ShouldReturn404() throws Exception {
        Long nonExistentNotificationId = 9999L;

        mockMvc.perform(put("/notifications/" + nonExistentNotificationId + "/mark-read"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testSubscribe_WhenUserIsAuthenticated_ShouldReturn200AndSseHeader() throws Exception {
        mockMvc.perform(get("/notifications/subscribe"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/event-stream"));
    }

    @Test
    void testSubscribe_WhenUserIsAnonymous_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/notifications/subscribe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetNotifications_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
        Long ownerId = 1L;
        mockMvc.perform(get("/notifications/user/" + ownerId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetNotifications_WithPagination_ShouldReturnCorrectPage() throws Exception {
        Long currentUserId = 1L;

        mockMvc.perform(get("/notifications/user/" + currentUserId)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(50L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetUnreadNotifications_WhenUserIsSelf_ShouldReturn200AndList() throws Exception {
        Long currentUserId = 1L;
        mockMvc.perform(get("/notifications/user/" + currentUserId + "/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(50L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetUnreadNotifications_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
        Long ownerId = 1L;
        mockMvc.perform(get("/notifications/user/" + ownerId + "/unread"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetUnreadNotificationCount_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
        Long ownerId = 1L;
        mockMvc.perform(get("/notifications/user/" + ownerId + "/unread-count"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testMarkAllAsRead_WhenUserIsSelf_ShouldReturn200() throws Exception {
        Long currentUserId = 1L;

        mockMvc.perform(put("/notifications/user/" + currentUserId + "/mark-all-read"))
                .andExpect(status().isOk())
                .andExpect(content().string("All notifications marked as read"));

        mockMvc.perform(get("/notifications/user/" + currentUserId + "/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testMarkAllAsRead_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
        Long ownerId = 1L;
        mockMvc.perform(put("/notifications/user/" + ownerId + "/mark-all-read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetNotificationsByType_WhenUserIsSelf_ShouldReturn200() throws Exception {
        Long currentUserId = 1L;
        String type = "POST_COMMENT";

        mockMvc.perform(get("/notifications/user/" + currentUserId + "/type/" + type))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(50L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetNotificationsByType_WhenInvalidType_ShouldReturn400() throws Exception {
        Long currentUserId = 1L;
        String type = "INVALID_TYPE";

        mockMvc.perform(get("/notifications/user/" + currentUserId + "/type/" + type))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetNotificationsByType_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
        Long ownerId = 1L;
        String type = "POST_COMMENT";
        mockMvc.perform(get("/notifications/user/" + ownerId + "/type/" + type))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCleanupOldNotifications_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/notifications/cleanup"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testCreateNotification_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/notifications/create")
                        .param("receiverId", "2")
                        .param("notificationType", "SYSTEM_ANNOUNCEMENT")
                        .param("title", "Test")
                        .param("content", "Test content"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testCleanupOldNotifications_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/notifications/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("Old notifications cleaned up"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testCreateNotification_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(post("/notifications/create")
                        .param("receiverId", "1")
                        .param("notificationType", "SYSTEM_ANNOUNCEMENT")
                        .param("title", "Admin Test")
                        .param("content", "Test content từ admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin Test"));
    }
}