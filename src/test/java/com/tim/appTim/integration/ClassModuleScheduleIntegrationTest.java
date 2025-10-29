package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.ClassModuleScheduleDTO;
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

import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ClassModuleScheduleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    private final String BASE_URL = "/schedules";

    private ClassModuleScheduleDTO createBaseDto(Long instructorId, LocalDate startDate) {
        ClassModuleScheduleDTO dto = new ClassModuleScheduleDTO();
        dto.setClassId(10L);
        dto.setModuleId(202L);
        dto.setInstructorId(instructorId);
        dto.setStartDate(startDate);
        dto.setEndDate(startDate.plusDays(7));
        return dto;
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSchedule_WhenValidData_ShouldReturn201Created() throws Exception {
        // DTO sẽ dùng Module ID 202 (chưa được lập lịch cho Class 10)
        ClassModuleScheduleDTO inputDto = createBaseDto(5L, LocalDate.of(2025, 12, 1));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated()) // Mong đợi 201
                .andExpect(jsonPath("$.classId").value(10L))
                .andExpect(jsonPath("$.moduleId").value(202L)); // ⬅️ KIỂM TRA MODULE MỚI
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSchedule_WhenInstructorIsConflicted_ShouldReturn400BadRequest() throws Exception {
        ClassModuleScheduleDTO conflictedDto = createBaseDto(5L, LocalDate.of(2025, 11, 10));
        conflictedDto.setEndDate(LocalDate.of(2025, 11, 25));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictedDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Giảng viên đã bị trùng lịch")));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSchedule_WhenEndDateIsBeforeStartDate_ShouldReturn400BadRequest() throws Exception {
        ClassModuleScheduleDTO invalidDateDto = createBaseDto(5L, LocalDate.of(2025, 11, 10));
        invalidDateDto.setEndDate(LocalDate.of(2025, 11, 1));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ngày kết thúc không thể trước ngày bắt đầu."));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSchedule_WhenModuleAlreadyScheduled_ShouldReturn400BadRequest() throws Exception {
        ClassModuleScheduleDTO duplicateDto = createBaseDto(6L, LocalDate.of(2026, 1, 1));
        duplicateDto.setModuleId(200L);
        duplicateDto.setClassId(10L);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Module này đã được lập lịch cho lớp học này."));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSchedule_WhenUpdatingSelf_ShouldAllowUpdate() throws Exception {
        Long scheduleId = 1000L;
        LocalDate newStartDate = LocalDate.of(2025, 10, 15);

        ClassModuleScheduleDTO updateDto = createBaseDto(5L, newStartDate);
        updateDto.setModuleId(200L);
        updateDto.setClassId(10L);

        mockMvc.perform(put(BASE_URL + "/{scheduleId}", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value(newStartDate.toString()));
    }
}