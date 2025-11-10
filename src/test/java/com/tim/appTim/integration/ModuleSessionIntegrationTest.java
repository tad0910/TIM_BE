package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.CreateModuleSessionRequest;
import com.tim.appTim.dto.UpdateModuleSessionRequest;
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
    @MockBean private PostService postService;

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionsByModule_WhenModuleExists_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/modules/200/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sessionNumber").value(1))
                .andExpect(jsonPath("$[0].id").value(300));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionsByModule_WhenModuleNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        mockMvc.perform(get("/modules/9999/sessions"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionById_WhenSessionExists_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/modules/sessions/300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buổi 1: Giới thiệu JPA"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionById_WhenSessionNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/modules/sessions/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenAdmin_ShouldReturn200() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(3);
        req.setTitle("Buổi 3: Spring Boot");
        req.setContent("Nội dung buổi 3");

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buổi 3: Spring Boot"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenDuplicateSessionNumber_ShouldReturn400() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(1);
        req.setTitle("Trùng số buổi");

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenModuleNotFound_ShouldReturn404() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(1);
        req.setTitle("Buổi 1");

        mockMvc.perform(post("/modules/9999/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void createSession_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(5);
        req.setTitle("Buổi 5");

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSession_WhenAnonymous_ShouldReturn401() throws Exception {
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(5);
        req.setTitle("Buổi 5");

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenAdmin_ShouldReturn200() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Tiêu đề đã cập nhật");
        req.setContent("Nội dung mới");

        mockMvc.perform(put("/modules/sessions/301")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tiêu đề đã cập nhật"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenDuplicateSessionNumber_ShouldReturn409() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setSessionNumber(1);

        mockMvc.perform(put("/modules/sessions/301")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenSessionNotFound_ShouldReturn404() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Test");

        mockMvc.perform(put("/modules/sessions/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void updateSession_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Test");

        mockMvc.perform(put("/modules/sessions/301")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSession_WhenAnonymous_ShouldReturn401() throws Exception {
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Test");

        mockMvc.perform(put("/modules/sessions/301")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenAdmin_ShouldReturn200() throws Exception {
        List<Long> sessionIds = List.of(303L);

        mockMvc.perform(put("/modules/201/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(201))
                .andExpect(jsonPath("$.sessions.length()").value(2));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenModuleNotFound_ShouldReturn404() throws Exception {
        List<Long> sessionIds = List.of(303L);
        mockMvc.perform(put("/modules/9999/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        List<Long> sessionIds = List.of(303L);
        mockMvc.perform(put("/modules/201/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addSessionsToModule_WhenAnonymous_ShouldReturn401() throws Exception {
        List<Long> sessionIds = List.of(303L);
        mockMvc.perform(put("/modules/201/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/modules/sessions/302"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete session successfully"))
                .andExpect(jsonPath("$.sessionId").value(302));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenSessionNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/modules/sessions/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/modules/sessions/302"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSession_WhenAnonymous_ShouldReturn401() throws Exception {
        mockMvc.perform(delete("/modules/sessions/302"))
                .andExpect(status().isUnauthorized());
    }
}