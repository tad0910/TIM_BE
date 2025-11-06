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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql") // Đảm bảo file này đã được cập nhật SQL ở trên
@ActiveProfiles("test")
public class ModuleSessionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private KeycloakSyncService keycloakSyncService;

    // --- Endpoint 1: GET /modules/{moduleId}/sessions ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionsByModule_WhenModuleExists_ShouldReturn200() throws Exception {
        // [SỬA] Endpoint này không cần auth.
        // [SỬA] Dùng ID 200 (từ test-data.sql) và kiểm tra dữ liệu mẫu (ID 300, 301)
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

    // --- Endpoint 2: GET /modules/sessions/{sessionId} ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionById_WhenSessionExists_ShouldReturn200() throws Exception {
        // [MỚI] Endpoint này không cần auth.
        mockMvc.perform(get("/modules/sessions/300")) // ID 300 từ SQL mẫu
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buổi 1: Giới thiệu JPA"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSessionById_WhenSessionNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        mockMvc.perform(get("/modules/sessions/9999"))
                .andExpect(status().isNotFound());
    }

    // --- Endpoint 3: POST /modules/{moduleId}/sessions ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenAdmin_ShouldReturn200() throws Exception {
        // [CŨ] Test này đã đúng, chỉ cần đảm bảo ID module (200) là đúng
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(3); // session 1, 2 đã có
        req.setTitle("Buổi 3: Spring Boot");
        req.setScheduledAt(LocalDateTime.of(2025, 12, 15, 9, 0));
        req.setEndDate(LocalDateTime.of(2025, 12, 15, 11, 0));

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buổi 3: Spring Boot"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenDuplicateSessionNumber_ShouldReturn400() throws Exception {
        // [CŨ] Test này giờ sẽ PASS vì SQL mẫu đã có session "1" (ID 300)
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(1); // Đã tồn tại trong module 200

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // Service ném BadRequestException
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenTimeOverlaps_ShouldReturn400() throws Exception {
        // [MỚI] Session 300 là 9h-11h ngày 2025-10-01
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(5);
        req.setTitle("Buổi 5: Bị trùng giờ");
        req.setScheduledAt(LocalDateTime.of(2025, 10, 1, 10, 0)); // Trùng (10h)
        req.setEndDate(LocalDateTime.of(2025, 10, 1, 12, 0));

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // Service ném BadRequestException
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenModuleNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(1);
        req.setTitle("Buổi 1");

        mockMvc.perform(post("/modules/9999/sessions") // Module 9999 không tồn tại
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSession_WhenInstructorNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(5);
        req.setTitle("Buổi 5");
        req.setInstructorId(9999L); // Giáo viên 9999 không tồn tại

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound()); // Service ném ResourceNotFound
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void createSession_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        // [MỚI] post_owner không có quyền 'module:create'
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(5);

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSession_WhenAnonymous_ShouldReturn401() throws Exception {
        // [MỚI]
        CreateModuleSessionRequest req = new CreateModuleSessionRequest();
        req.setSessionNumber(5);

        mockMvc.perform(post("/modules/200/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- Endpoint 4: PUT /modules/sessions/{sessionId} ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenAdmin_ShouldReturn200() throws Exception {
        // [MỚI]
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Tiêu đề đã cập nhật");
        req.setStatus("ongoing");

        mockMvc.perform(put("/modules/sessions/301") // Cập nhật session 301
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tiêu đề đã cập nhật"))
                .andExpect(jsonPath("$.status").value("ongoing"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenDuplicateSessionNumber_ShouldReturn409() throws Exception {
        // [MỚI] Cập nhật session 301, đổi sessionNumber thành 1 (đã bị 300 dùng)
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setSessionNumber(1); // Trùng với 300

        mockMvc.perform(put("/modules/sessions/301")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict()); // Service ném ConflictException (409)
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSession_WhenSessionNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Test");

        mockMvc.perform(put("/modules/sessions/9999") // Session 9999 không tồn tại
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void updateSession_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        // [MỚI] post_owner không có quyền 'module:update'
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Test");

        mockMvc.perform(put("/modules/sessions/301")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateSession_WhenAnonymous_ShouldReturn401() throws Exception {
        // [MỚI]
        UpdateModuleSessionRequest req = new UpdateModuleSessionRequest();
        req.setTitle("Test");

        mockMvc.perform(put("/modules/sessions/301")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- Endpoint 5: PUT /modules/{moduleId}/sessions ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenAdmin_ShouldReturn200() throws Exception {
        // [MỚI] Gán session 303 (đang null module) vào module 201
        List<Long> sessionIds = List.of(303L);

        mockMvc.perform(put("/modules/201/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(201))
                .andExpect(jsonPath("$.sessions.length()").value(2)); // 302 (cũ) và 303 (mới)
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenModuleNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        List<Long> sessionIds = List.of(303L);
        mockMvc.perform(put("/modules/9999/sessions") // Module 9999 không tồn tại
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void addSessionsToModule_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        // [MỚI]
        List<Long> sessionIds = List.of(303L);
        mockMvc.perform(put("/modules/201/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addSessionsToModule_WhenAnonymous_ShouldReturn401() throws Exception {
        // [MỚI]
        List<Long> sessionIds = List.of(303L);
        mockMvc.perform(put("/modules/201/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionIds)))
                .andExpect(status().isUnauthorized());
    }

    // --- Endpoint 6: DELETE /modules/sessions/{sessionId} ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenAdmin_ShouldReturn200() throws Exception {
        // [MỚI] Yêu cầu phải thêm quyền 'module:delete' vào SQL
        mockMvc.perform(delete("/modules/sessions/302"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete session successfully"))
                .andExpect(jsonPath("$.sessionId").value(302));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenSessionNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        mockMvc.perform(delete("/modules/sessions/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteSession_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        // [MỚI] post_owner không có quyền 'module:delete'
        mockMvc.perform(delete("/modules/sessions/302"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSession_WhenAnonymous_ShouldReturn401() throws Exception {
        // [MỚI]
        mockMvc.perform(delete("/modules/sessions/302"))
                .andExpect(status().isUnauthorized());
    }

    // --- Endpoint 7: PUT /modules/sessions/{sessionId}/instructor ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(put("/modules/sessions/300/instructor")
                        .param("instructorId", "6")) // ID 6 = giaovien2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorId").value(6))
                .andExpect(jsonPath("$.instructorName").value("giaovien2")); // Giả sử gv2 chưa có first/last name
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenSessionNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        mockMvc.perform(put("/modules/sessions/9999/instructor")
                        .param("instructorId", "6"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenInstructorNotFound_ShouldReturn404() throws Exception {
        // [MỚI]
        mockMvc.perform(put("/modules/sessions/300/instructor")
                        .param("instructorId", "9999")) // GV 9999 không tồn tại
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void assignInstructor_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        // [MỚI]
        mockMvc.perform(put("/modules/sessions/300/instructor")
                        .param("instructorId", "6"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignInstructor_WhenAnonymous_ShouldReturn401() throws Exception {
        // [MỚI]
        mockMvc.perform(put("/modules/sessions/300/instructor")
                        .param("instructorId", "6"))
                .andExpect(status().isUnauthorized());
    }
}