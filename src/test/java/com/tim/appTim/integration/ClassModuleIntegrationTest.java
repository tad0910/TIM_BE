package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.ClassModuleTeacherDTO;
import com.tim.appTim.entity.ClassModuleTeacher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class ClassModuleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin_user", authorities = "class:update_all")
    void testCreateModulesFromProgram_ShouldReturnCreated() throws Exception {
        Long classId = 10L; // From test-data.sql

        // This endpoint creates modules for the class based on the program
        mockMvc.perform(post("/classes/{classId}/modules/from-program", classId))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin_user")
    void testGetClassModules_ShouldReturnOk() throws Exception {
        Long classId = 10L;
        mockMvc.perform(get("/classes/{classId}/modules", classId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = "class:update_all")
    void testAssignTeacher_ShouldReturnCreated() throws Exception {
        Long classId = 10L;
        Long classModuleId = 500L; // From test-data.sql
        Long teacherId = 6L; // giaovien2

        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setUserId(teacherId);
        dto.setRole(ClassModuleTeacher.TeacherRole.TEACHER);

        mockMvc.perform(post("/classes/{classId}/modules/{classModuleId}/teachers", classId, classModuleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = "class:update_all")
    void testUpdateTeacherRole_ShouldReturnOk() throws Exception {
        Long classId = 10L;
        Long classModuleId = 500L;
        Long teacherId = 5L; // giaovien1, already assigned in test-data.sql

        mockMvc.perform(put("/classes/{classId}/modules/{classModuleId}/teachers/{userId}/role", classId, classModuleId,
                teacherId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "TEACHER"))))
                .andExpect(status().isOk());
    }
}
