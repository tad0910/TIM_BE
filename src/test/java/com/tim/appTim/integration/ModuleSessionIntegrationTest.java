package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.CreateModuleSessionRequest;
import com.tim.appTim.dto.UpdateModuleSessionRequest;
import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ModuleSessionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private KeycloakSyncService keycloakSyncService;

    private final String BASE_URL = "/modules/200/sessions";

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenValid_ShouldReturn200() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(3);
        req.setTitle("Buổi 3: Spring Boot");
        req.setScheduledAt(LocalDateTime.of(2025, 12, 15, 9, 0));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buổi 3: Spring Boot"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenDuplicateSessionNumber_ShouldReturn400() throws Exception {
        // Session số 1 đã tồn tại trong test-data.sql (module 200)
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(1);
        req.setTitle("Duplicate Session");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getSessions_WhenTeacherOfClass_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sessionNumber").value(1));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionsByModule_WhenModuleDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/modules/999/sessions"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /modules/sessions/{sessionId} - getSessionById ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionById_WhenSessionExists_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/modules/sessions/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionById_WhenSessionDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/modules/sessions/9999"))
                .andExpect(status().isNotFound());
    }

    // ========== POST /modules/{moduleId}/sessions - createSession (Additional tests) ==========
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void createSession_WhenUserWithoutPermission_ShouldReturn403() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(5);
        req.setTitle("Test Session");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenModuleDoesNotExist_ShouldReturn404() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(1);
        req.setTitle("Test Session");

        mockMvc.perform(post("/modules/999/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenSessionNumberIsNull_ShouldReturn400() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setTitle("Test Session");
        // sessionNumber is null

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ========== PUT /modules/sessions/{sessionId} - updateSession ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenAdmin_ShouldReturn200() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Updated Title");

        mockMvc.perform(put("/modules/sessions/1000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void updateSession_WhenUserWithoutPermission_ShouldReturn403() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Updated Title");

        mockMvc.perform(put("/modules/sessions/1000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenSessionDoesNotExist_ShouldReturn404() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Updated Title");

        mockMvc.perform(put("/modules/sessions/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ========== PUT /modules/{moduleId}/sessions - addSessionsToModule ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenAdmin_ShouldReturn200() throws Exception {
        List<Long> sessionIds = Arrays.asList(1000L, 1001L);

        mockMvc.perform(put("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenUserWithoutPermission_ShouldReturn403() throws Exception {
        List<Long> sessionIds = Arrays.asList(1000L);

        mockMvc.perform(put("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenModuleDoesNotExist_ShouldReturn404() throws Exception {
        List<Long> sessionIds = Arrays.asList(1000L);

        mockMvc.perform(put("/modules/999/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE /modules/sessions/{sessionId} - deleteSession ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenAdmin_ShouldReturn200() throws Exception {
        // Tạo session mới trước khi xóa
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(99);
        req.setTitle("Session to Delete");

        String createResponse = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Lấy session ID từ response
        Long sessionId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/modules/sessions/" + sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete session successfully"))
                .andExpect(jsonPath("$.sessionId").value(sessionId));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenUserWithoutPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/modules/sessions/1000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"module:delete"})
    void deleteSession_WhenSessionDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/modules/sessions/9999"))
                .andExpect(status().isNotFound());
    }

    // ========== PUT /modules/sessions/{sessionId}/instructor - assignInstructor ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/modules/sessions/1000/instructor")
                        .param("instructorId", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenUserWithoutPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(put("/modules/sessions/1000/instructor")
                        .param("instructorId", "5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenSessionDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(put("/modules/sessions/9999/instructor")
                        .param("instructorId", "5"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenInstructorDoesNotExist_ShouldReturn404() throws Exception {
        mockMvc.perform(put("/modules/sessions/1000/instructor")
                        .param("instructorId", "9999"))
                .andExpect(status().isNotFound());
    }
}