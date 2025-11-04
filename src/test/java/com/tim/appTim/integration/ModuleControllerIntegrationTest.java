package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.ModuleDTO;
import com.tim.appTim.dto.CreateModuleSessionRequest;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.ModuleService;
import com.tim.appTim.service.ModuleSessionService;
import com.tim.appTim.exception.ResourceNotFoundException; // Đảm bảo bạn đã import
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql") // Cần để load quyền (authorities)
@ActiveProfiles("test")
public class ModuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    // Giả lập các service
    @MockBean
    private ModuleService moduleService;

    @MockBean
    private ModuleSessionService moduleSessionService;

    private final String BASE_URL = "/module";
    private ModuleDTO testModule;

    @BeforeEach
    void setUp() {
        // Tạo một đối tượng ModuleDTO mẫu
        testModule = new ModuleDTO();
        testModule.setId(200);
        testModule.setName("Module Spring JPA");
        testModule.setDescription("Hướng dẫn lập trình cơ sở dữ liệu.");
    }

    // --- Test GET (Không cần quyền) ---

    @Test
    @WithMockUser // Chỉ cần đăng nhập
    void getAllModules_ShouldReturn200() throws Exception {
        // Giả lập service trả về 1 list
        when(moduleService.getAllModules()).thenReturn(List.of(testModule));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(200));
    }

    @Test
    @WithMockUser
    void getModuleById_WhenModuleExists_ShouldReturn200() throws Exception {
        // Giả lập service trả về 1 module
        when(moduleService.getModuleById(200)).thenReturn(testModule);

        mockMvc.perform(get(BASE_URL + "/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Module Spring JPA"));
    }

    @Test
    @WithMockUser
    void getModuleById_WhenModuleNotFound_ShouldReturn404() throws Exception {
        // Giả lập service ném lỗi 404
        when(moduleService.getModuleById(999)).thenThrow(new ResourceNotFoundException("Module not found"));

        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    // --- Test POST (Cần quyền) ---

    @Test
    // Dùng admin_user vì test-data.sql đã gán quyền 'module:create' (giả định)
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createModule_WhenAdmin_ShouldReturn200() throws Exception {
        // Giả lập service trả về module đã tạo
        when(moduleService.createModule(any(ModuleDTO.class))).thenReturn(testModule);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testModule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // User thường
    void createModule_WhenUser_ShouldReturn403() throws Exception {
        // Không cần mock service vì @PreAuthorize sẽ chặn

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testModule)))
                .andExpect(status().isForbidden()); // Mong đợi 403
    }

    // --- Test PUT (Cần quyền) ---

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    @WithMockUser(authorities = "module:update") // Cách khác để gán quyền
    void updateModule_WhenAdmin_ShouldReturn200() throws Exception {
        // Giả lập service trả về module đã cập nhật
        when(moduleService.updateModule(eq(200), any(ModuleDTO.class))).thenReturn(testModule);

        mockMvc.perform(put(BASE_URL + "/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testModule)))
                .andExpect(status().isOk());
    }

    // --- Test DELETE (Cần quyền) ---

    @Test
    @WithMockUser(authorities = "module:delete") // Gán quyền
    void deleteModule_WhenAdmin_ShouldReturn200() throws Exception {
        // Giả lập service xóa thành công
        doNothing().when(moduleService).deleteModule(200);

        mockMvc.perform(delete(BASE_URL + "/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete module successfully"));
    }

    // --- Test Thêm Session (Cần quyền) ---

    @Test
    @WithMockUser(authorities = "module:create") // Dùng quyền 'module:create'
    void addSessionToModule_WhenAdmin_ShouldReturn200() throws Exception {
        CreateModuleSessionRequest sessionRequest = new CreateModuleSessionRequest();
        // ... (set data cho sessionRequest) ...

        // Giả lập service
        doNothing().when(moduleSessionService).createSession(eq(200), any(CreateModuleSessionRequest.class));
        when(moduleService.getModuleById(200)).thenReturn(testModule);

        mockMvc.perform(put(BASE_URL + "/200/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200));
    }
}