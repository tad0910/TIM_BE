package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.ModuleDTO;
import com.tim.appTim.dto.CreateModuleSessionRequest;
import com.tim.appTim.dto.ModuleSessionDTO;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.ModuleService;
import com.tim.appTim.service.ModuleSessionService;
import com.tim.appTim.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @MockBean
    private ModuleService moduleService;

    @MockBean
    private ModuleSessionService moduleSessionService;

    @SpyBean
    private UserService userService;

    private final String BASE_URL = "/module";
    private ModuleDTO testModule;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testModule = new ModuleDTO();
        testModule.setId(200);
        testModule.setName("Module Spring JPA");
        testModule.setDescription("Hướng dẫn lập trình cơ sở dữ liệu.");
    }

    @Test
    @WithMockUser
    void getAllModules_ShouldReturn200() throws Exception {
        when(moduleService.getAllModules()).thenReturn(List.of(testModule));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(200));
    }

    @Test
    @WithMockUser
    void getModuleById_WhenModuleExists_ShouldReturn200() throws Exception {
        when(moduleService.getModuleById(200)).thenReturn(testModule);

        mockMvc.perform(get(BASE_URL + "/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Module Spring JPA"));
    }

    @Test
    @WithMockUser
    void getModuleById_WhenModuleNotFound_ShouldReturn404() throws Exception {
        when(moduleService.getModuleById(999)).thenThrow(new ResourceNotFoundException("Module not found"));

        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createModule_WhenAdmin_ShouldReturn200() throws Exception {
        when(moduleService.createModule(any(ModuleDTO.class))).thenReturn(testModule);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testModule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void createModule_WhenUser_ShouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testModule)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateModule_WhenAdmin_ShouldReturn200() throws Exception {
        when(moduleService.updateModule(eq(200), any(ModuleDTO.class))).thenReturn(testModule);

        mockMvc.perform(put(BASE_URL + "/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testModule)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "module:delete")
    void deleteModule_WhenAdmin_ShouldReturn200() throws Exception {
        doNothing().when(moduleService).deleteModule(200);

        mockMvc.perform(delete(BASE_URL + "/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delete module successfully"));
    }

    @Test
    @WithMockUser(authorities = "module:create")
    void addSessionToModule_WhenAdmin_ShouldReturn200() throws Exception {
        CreateModuleSessionRequest sessionRequest = new CreateModuleSessionRequest();
        when(moduleSessionService.createSession(eq(200), any(CreateModuleSessionRequest.class)))
                .thenReturn(new ModuleSessionDTO());

        when(moduleService.getModuleById(200)).thenReturn(testModule);

        mockMvc.perform(put(BASE_URL + "/200/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200));
    }
}