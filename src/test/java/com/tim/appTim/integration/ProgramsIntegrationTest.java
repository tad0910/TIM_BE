package com.tim.appTim.integration;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.Programs;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ProgramsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private KeycloakSyncService keycloakSyncService;
    @MockBean private PostService postService;

    private final String BASE_URL = "/programs";

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getAllPrograms_WhenPublic_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())           
                .andExpect(jsonPath("$.content[0].name").value("Khóa học Backend"))
                .andExpect(jsonPath("$.totalElements").value(1));     
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
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
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
        String body = "[200,201,202]";

        mockMvc.perform(put(BASE_URL + "/100/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules").isArray())
                .andExpect(jsonPath("$.modules.length()").value(3));
    }



    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetProgramById_WhenExists_ShouldReturn200() throws Exception {
        Integer programId = 100;
        mockMvc.perform(get(BASE_URL + "/" + programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(programId))
                .andExpect(jsonPath("$.name").value("Khóa học Backend"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testGetProgramById_WhenNotFound_ShouldReturn404() throws Exception {
        Integer programId = 9999;
        mockMvc.perform(get(BASE_URL + "/" + programId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProgram_WhenNotAuthenticated_ShouldReturn401() throws Exception {
        Programs program = new Programs();
        program.setName("Test 401");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(program)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createProgram_WhenInvalidBody_ShouldReturn422() throws Exception {
        String invalidBody = "{\"description\":\"Một mô tả\"}";

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateProgram_WhenAdmin_ShouldReturn200() throws Exception {
        Integer programId = 100;
        Programs updatedProgram = new Programs();
        updatedProgram.setName("Tên Mới Sau Khi Update");
        updatedProgram.setDescription("Mô tả mới");

        mockMvc.perform(put(BASE_URL + "/" + programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedProgram)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tên Mới Sau Khi Update"));
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void updateProgram_WhenNotAdmin_ShouldReturn403() throws Exception {
        Integer programId = 100;
        Programs updatedProgram = new Programs();
        updatedProgram.setName("Sẽ bị cấm");

        mockMvc.perform(put(BASE_URL + "/" + programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedProgram)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProgram_WhenNotAuthenticated_ShouldReturn401() throws Exception {
        Integer programId = 100;
        Programs updatedProgram = new Programs();
        updatedProgram.setName("Sẽ bị 401");

        mockMvc.perform(put(BASE_URL + "/" + programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedProgram)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateProgram_WhenNotFound_ShouldReturn404() throws Exception {
        Integer programId = 9999;
        Programs updatedProgram = new Programs();
        updatedProgram.setName("Update vào hư vô");

        mockMvc.perform(put(BASE_URL + "/" + programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updatedProgram)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void addModulesToProgram_WhenNotAdmin_ShouldReturn403() throws Exception {
        String body = "[201, 202]";
        mockMvc.perform(put(BASE_URL + "/100/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void addModulesToProgram_WhenNotAuthenticated_ShouldReturn401() throws Exception {
        String body = "[201, 202]";
        mockMvc.perform(put(BASE_URL + "/100/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addModulesToProgram_WhenProgramNotFound_ShouldReturn404() throws Exception {
        String body = "[201, 202]";
        mockMvc.perform(put(BASE_URL + "/9999/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void addModulesToProgram_WhenModuleNotFound_ShouldReturn404() throws Exception {
        String body = "[200, 9999]";
        mockMvc.perform(put(BASE_URL + "/100/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteProgram_WhenInUse_ShouldReturn409() throws Exception {
        Integer programId = 100;
        mockMvc.perform(delete(BASE_URL + "/" + programId))
                .andExpect(status().isConflict());
    }

    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteProgram_WhenNotFound_ShouldReturn404() throws Exception {
        Integer programId = 9999;
        mockMvc.perform(delete(BASE_URL + "/" + programId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void deleteProgram_WhenNotAdmin_ShouldReturn403() throws Exception {
        Integer programId = 100;
        mockMvc.perform(delete(BASE_URL + "/" + programId))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProgram_WhenNotAuthenticated_ShouldReturn401() throws Exception {
        Integer programId = 100;
        mockMvc.perform(delete(BASE_URL + "/" + programId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteProgram_WhenValid_ShouldReturn204() throws Exception {
        Programs program = new Programs();
        program.setName("Program để Xóa");
        String response = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(program)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ProgramsDTO createdProgram = new ObjectMapper().readValue(response, ProgramsDTO.class);
        Integer newProgramId = createdProgram.getId();

        mockMvc.perform(delete(BASE_URL + "/" + newProgramId))
                .andExpect(status().isNoContent());
    }
}
