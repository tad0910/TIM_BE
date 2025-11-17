package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.AttendanceMarkDto;
import com.tim.appTim.dto.MarkAttendanceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql") 
@ActiveProfiles("test")
public class AttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    private final String BASE_URL = "/attendance";

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void openAttendanceSession_WhenAdminOpens_ShouldReturn200() throws Exception {
        Long scheduleId = 1000L; 
        Map<String, Integer> request = Map.of("teacherId", 5); 

        mockMvc.perform(post(BASE_URL + "/schedules/" + scheduleId + "/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.openedBy").value(5));
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void openAttendanceSession_WhenAssignedTeacherOpens_ShouldReturn200() throws Exception {
        Long scheduleId = 1000L; 
        Map<String, Integer> request = Map.of("teacherId", 5); 

        mockMvc.perform(post(BASE_URL + "/schedules/" + scheduleId + "/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(scheduleId));
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void openAttendanceSession_WhenUnassignedTeacherOpens_ShouldReturn403() throws Exception {
        Long scheduleId = 1000L; 
        Map<String, Integer> request = Map.of("teacherId", 6); 
        mockMvc.perform(post(BASE_URL + "/schedules/" + scheduleId + "/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void openAttendanceSession_WhenSessionAlreadyOpen_ShouldReturn409() throws Exception {
        Long scheduleId = 1001L; 
        Map<String, Integer> request = Map.of("teacherId", 5);

        mockMvc.perform(post(BASE_URL + "/schedules/" + scheduleId + "/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()) 
                .andExpect(jsonPath("$.message").value("Buổi điểm danh đã được mở trước đó."));
    }


    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void markAttendance_WhenAssignedTeacherMarks_ShouldReturn200() throws Exception {
        Long scheduleId = 1001L; 
        
        AttendanceMarkDto markDto = new AttendanceMarkDto();
        markDto.setStudentId(1);
        // Status must match the enum used in the domain model (PRESENT, ABSENT, LATE, EXCUSED, ...)
        // Using PRESENT here so the request is valid and we can assert authorization/business logic.
        markDto.setStatus("PRESENT");
        markDto.setNotes("GV1 marked"); 

        MarkAttendanceRequest request = new MarkAttendanceRequest();
        request.setTeacherId(5); 
        request.setRecords(List.of(markDto)); 

        mockMvc.perform(post(BASE_URL + "/schedules/" + scheduleId + "/mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void markAttendance_WhenUnassignedTeacherMarks_ShouldReturn403() throws Exception {
        Long scheduleId = 1001L; 
        
        AttendanceMarkDto markDto = new AttendanceMarkDto();
        markDto.setStudentId(1);
        // Use a valid status so this test focuses on the \"unassigned teacher\" authorization branch
        markDto.setStatus("PRESENT");
        markDto.setNotes("GV2 marked");
        
        MarkAttendanceRequest request = new MarkAttendanceRequest();
        request.setTeacherId(6); 
        request.setRecords(List.of(markDto)); 

        mockMvc.perform(post(BASE_URL + "/schedules/" + scheduleId + "/mark") 
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void markAttendance_WhenSessionNotOpen_ShouldReturn404() throws Exception {
        Long scheduleId = 1000L; 
        
        AttendanceMarkDto markDto = new AttendanceMarkDto();
        markDto.setStudentId(1);
        // Use a valid status so this test focuses on the \"session not open\" branch
        markDto.setStatus("PRESENT");
          MarkAttendanceRequest request = new MarkAttendanceRequest();
        request.setTeacherId(3); 
        request.setRecords(List.of(markDto)); 

        mockMvc.perform(post(BASE_URL + "/schedules/" + scheduleId + "/mark") 
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()) 
                .andExpect(jsonPath("$.message").value("Chưa mở buổi điểm danh."));
    }


    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getAttendanceHistory_WhenAdmin_ShouldReturn200() throws Exception {
        Integer classId = 10; 

        mockMvc.perform(get(BASE_URL + "/history/" + classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getAttendanceHistory_WhenTeacher_ShouldReturn403() throws Exception {
        Integer classId = 10;

        mockMvc.perform(get(BASE_URL + "/history/" + classId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getAttendanceStats_WhenAdmin_ShouldReturn200() throws Exception {
        Integer classId = 10;

        mockMvc.perform(get(BASE_URL + "/stats/" + classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getAttendanceStats_WhenTeacher_ShouldReturn403() throws Exception {
        Integer classId = 10;


        mockMvc.perform(get(BASE_URL + "/stats/" + classId))
                .andExpect(status().isForbidden());
    }
}