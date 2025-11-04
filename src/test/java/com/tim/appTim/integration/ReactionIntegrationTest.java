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

    // ⬅️ KHAI BÁO BIẾN THIẾU
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
    void getSchedulesByClass_WhenFilteredByValidDateRange_ShouldReturnOneSchedule() throws Exception {
        Long classId = 10L;

        String startDate = "2025-11-01";
        String endDate = "2025-11-30";

        mockMvc.perform(get(BASE_URL + "/class/{classId}", classId)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1000L))
                .andExpect(jsonPath("$[0].startDate").value("2025-11-01"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByClass_WhenFilteredByEmptyDateRange_ShouldReturnEmptyList() throws Exception {
        Long classId = 10L;

        String startDate = "2025-10-01";
        String endDate = "2025-10-31";

        mockMvc.perform(get(BASE_URL + "/class/{classId}", classId)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByInstructor_WhenFilteredByValidDateRange_ShouldReturnSchedule() throws Exception {
        Long instructorId = 5L;

        String startDate = "2025-11-01";
        String endDate = "2025-11-30";

        mockMvc.perform(get(BASE_URL + "/instructor/{instructorId}", instructorId)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].instructorId").value(5L));
    }
}