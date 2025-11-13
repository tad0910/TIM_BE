package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.ClassModuleScheduleDTO;
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

import java.time.LocalDateTime;
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

    @MockBean
    private PostService postService;

    private final String BASE_URL = "/schedules";

    private ClassModuleScheduleDTO createBaseDto(Long instructorId, LocalDateTime startDate) {
        ClassModuleScheduleDTO dto = new ClassModuleScheduleDTO();
        dto.setClassId(10L);
        dto.setModuleId(202L);
        dto.setInstructorId(instructorId);
        dto.setStartDate(startDate);
        dto.setEndDate(startDate.plusDays(7));
        return dto;
    }

    // @Test
    // @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    // void createSchedule_WhenValidData_ShouldReturn201Created() throws Exception {
    //     ClassModuleScheduleDTO inputDto = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));

    //     mockMvc.perform(post(BASE_URL)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(inputDto)))
    //             .andExpect(status().isCreated()) 
    //             .andExpect(jsonPath("$.classId").value(10L))
    //             .andExpect(jsonPath("$.moduleId").value(202L));
    // }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSchedule_WhenInstructorIsConflicted_ShouldReturn400BadRequest() throws Exception {
        ClassModuleScheduleDTO conflictedDto = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));
        conflictedDto.setEndDate(LocalDateTime.of(2025, 11, 1, 0, 0, 0));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictedDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Giảng viên đã bị trùng lịch")));
    }

    // @Test
    // @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    // void createSchedule_WhenEndDateIsBeforeStartDate_ShouldReturn400BadRequest() throws Exception {
    //     ClassModuleScheduleDTO invalidDateDto = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));
    //     invalidDateDto.setEndDate(LocalDateTime.of(2025, 11, 1, 0, 0, 0));

    //     mockMvc.perform(post(BASE_URL)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(invalidDateDto)))
    //             .andExpect(status().isBadRequest())
    //             .andExpect(jsonPath("$.message").value("Ngày kết thúc không thể trước ngày bắt đầu."));
    // }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void createSchedule_WhenModuleAlreadyScheduled_ShouldReturn400BadRequest() throws Exception {
        ClassModuleScheduleDTO duplicateDto = createBaseDto(6L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));
        duplicateDto.setModuleId(200L);
        duplicateDto.setClassId(10L);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Module này đã được lập lịch cho lớp học này")));
    }

    // @Test
    // @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    // void updateSchedule_WhenUpdatingSelf_ShouldAllowUpdate() throws Exception {
    //     Long scheduleId = 1000L;
    //     LocalDateTime newStartDate = LocalDateTime.of(2025, 11, 1, 0, 0, 0);

    //     ClassModuleScheduleDTO updateDto = createBaseDto(5L, newStartDate);
    //     updateDto.setModuleId(200L);
    //     updateDto.setClassId(10L);

    //     mockMvc.perform(put(BASE_URL + "/{scheduleId}", scheduleId)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(updateDto)))
    //             .andExpect(status().isOk())
    //             .andExpect(jsonPath("$.startDate").value(newStartDate.toString()));
    // }

    // ========== POST /schedules - Authorization tests ==========
    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void createSchedule_WhenUserDoesNotHavePermission_ShouldReturn403() throws Exception {
        ClassModuleScheduleDTO inputDto = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isForbidden());
    }

    // ========== PUT /schedules/{scheduleId} - Additional tests ==========
    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void updateSchedule_WhenUserDoesNotHavePermission_ShouldReturn403() throws Exception {
        Long scheduleId = 1000L;
        ClassModuleScheduleDTO updateDto = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));

        mockMvc.perform(put(BASE_URL + "/{scheduleId}", scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void updateSchedule_WhenScheduleDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentScheduleId = 9999L;
        ClassModuleScheduleDTO updateDto = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));

        mockMvc.perform(put(BASE_URL + "/{scheduleId}", nonExistentScheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    // @Test
    // @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    // void updateSchedule_WhenInstructorIsConflicted_ShouldReturn400BadRequest() throws Exception {
    //     ClassModuleScheduleDTO firstSchedule = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));
    //     firstSchedule.setEndDate(LocalDateTime.of(2025, 11, 1, 0, 0, 0));
    //     firstSchedule.setModuleId(202L); 
    //     firstSchedule.setClassId(11L); 
        
    //     mockMvc.perform(post(BASE_URL)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(firstSchedule)))
    //             .andExpect(status().isCreated());

    //     Long scheduleId = 1000L;
    //     ClassModuleScheduleDTO conflictedDto = createBaseDto(5L, LocalDateTime.of(2025, 11, 1, 0, 0, 0));
    //     conflictedDto.setEndDate(  LocalDateTime.of(2025, 11, 1, 0, 0, 0));
    //     conflictedDto.setModuleId(200L);
    //     conflictedDto.setClassId(10L);

    //     mockMvc.perform(put(BASE_URL + "/{scheduleId}", scheduleId)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(conflictedDto)))
    //             .andExpect(status().isBadRequest())
    //             .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Giảng viên đã bị trùng lịch")));
    // }

    // ========== GET /schedules/class/{classId} ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByClass_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        Long classId = 10L;

        mockMvc.perform(get(BASE_URL + "/class/{classId}", classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getSchedulesByClass_WhenUserIsTeacher_ShouldReturn200() throws Exception {
        Long classId = 10L;

        mockMvc.perform(get(BASE_URL + "/class/{classId}", classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByClass_WithDateFilters_ShouldReturn200() throws Exception {
        Long classId = 10L;
        LocalDateTime startDate = LocalDateTime.of(2025, 11, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 11, 1, 0, 0, 0);

        mockMvc.perform(get(BASE_URL + "/class/{classId}", classId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByClass_WhenClassDoesNotExist_ShouldReturn200WithEmptyList() throws Exception {
        Long nonExistentClassId = 999L;

        mockMvc.perform(get(BASE_URL + "/class/{classId}", nonExistentClassId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ========== GET /schedules/instructor/{instructorId} ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByInstructor_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long instructorId = 5L;

        mockMvc.perform(get(BASE_URL + "/instructor/{instructorId}", instructorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getSchedulesByInstructor_WhenUserIsSelf_ShouldReturn200() throws Exception {
        Long instructorId = 5L;

        mockMvc.perform(get(BASE_URL + "/instructor/{instructorId}", instructorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void getSchedulesByInstructor_WhenUserIsNotSelfAndNotAdmin_ShouldReturn403() throws Exception {
        Long instructorId = 5L;

        mockMvc.perform(get(BASE_URL + "/instructor/{instructorId}", instructorId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByInstructor_WithDateFilters_ShouldReturn200() throws Exception {
        Long instructorId = 5L;
        LocalDateTime startDate = LocalDateTime.of(2025, 11, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 11, 1, 0, 0, 0);

        mockMvc.perform(get(BASE_URL + "/instructor/{instructorId}", instructorId)
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSchedulesByInstructor_WhenInstructorDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentInstructorId = 999L;

        mockMvc.perform(get(BASE_URL + "/instructor/{instructorId}", nonExistentInstructorId))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE /schedules/{scheduleId} ==========
    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteSchedule_WhenUserHasPermission_ShouldReturn200() throws Exception {
        Long scheduleId = 1001L;

        mockMvc.perform(delete(BASE_URL + "/{scheduleId}", scheduleId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void deleteSchedule_WhenUserDoesNotHavePermission_ShouldReturn403() throws Exception {
        Long scheduleId = 1000L;

        mockMvc.perform(delete(BASE_URL + "/{scheduleId}", scheduleId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteSchedule_WhenScheduleDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentScheduleId = 9999L;

        mockMvc.perform(delete(BASE_URL + "/{scheduleId}", nonExistentScheduleId))
                .andExpect(status().isNotFound());
    }
}