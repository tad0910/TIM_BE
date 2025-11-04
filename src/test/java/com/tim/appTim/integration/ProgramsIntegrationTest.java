package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.Programs;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ProgramsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private KeycloakSyncService keycloakSyncService;

    private final String BASE_URL = "/programs";

    @Test
    void getAllPrograms_WhenPublic_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Chương trình Java"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createProgram_WhenValid_ShouldReturn200() throws Exception {
        Programs program = new Programs();
        program.setName("Python Cơ Bản");
        program.setDescription("Học Python từ A-Z");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(program)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Python Cơ Bản"));
    }

    @Test
    @WithUserDetails(value = "teacher_user", userDetailsServiceBeanName = "userService")
    void createProgram_WhenNotAdmin_ShouldReturn403() throws Exception {
        Programs program = new Programs();
        program.setName("Không được tạo");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(program)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addModulesToProgram_ShouldReturn200() throws Exception {
        String body = "[201, 202]";

        mockMvc.perform(put(BASE_URL + "/1/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules").isArray())
                .andExpect(jsonPath("$.modules.length()").value(2));
    }
}