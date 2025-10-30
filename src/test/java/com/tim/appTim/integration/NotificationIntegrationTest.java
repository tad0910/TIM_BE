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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetNotifications_WhenUserIsAuthenticated_ShouldReturn200AndList() throws Exception {
        Long currentUserId = 1L;

        mockMvc.perform(get("/notifications/user/" + currentUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(50L))
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
}
