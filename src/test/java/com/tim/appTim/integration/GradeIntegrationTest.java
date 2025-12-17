package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.BatchGradeUpdateDTO;
import com.tim.appTim.dto.StudentScoreEntryDTO;
import com.tim.appTim.service.GradeService;
import com.tim.appTim.service.NotificationService;
import com.tim.appTim.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
class GradeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Dùng SpyBean để chạy logic thật của Service
    @SpyBean
    private GradeService gradeService;

    // Dùng SpyBean cho UserService để lấy User thật từ DB (quan trọng để fix lỗi
    // 500)
    @SpyBean
    private UserService userService;

    // Mock Notification vì đây là service bên ngoài (email/push), không cần test
    // logic DB
    @MockBean
    private NotificationService notificationService;

    private final String BASE_URL = "/grades";

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Giả lập notification thành công để không ảnh hưởng luồng test
        doReturn(null).when(notificationService).createNotification(any(), any(), any(), any(), any(), any(), any());
    }

    // --- TEST GET MY GRADES ---

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyGradesInModule_WhenStudentIsSelf_ShouldReturn200() throws Exception {
        // post_owner (id=1) là sinh viên lớp module 500
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.theoryScore").value(8.0)); // Dữ liệu từ test-data.sql
    }

    @Test
    @WithUserDetails(value = "stranger_user", userDetailsServiceBeanName = "userService")
    void getMyGradesInModule_WhenUserNotInClass_ShouldReturnForbidden() throws Exception {
        // stranger_user (id=4) không có trong bảng class_members
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyGradesInModule_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isUnauthorized());
    }

    // --- TEST GRADEBOOK (TEACHER) ---

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getModuleGradebook_WhenTeacherIsAuthorized_ShouldReturn200() throws Exception {
        // giaovien1 (id=5) là giáo viên của module 500
        mockMvc.perform(get(BASE_URL + "/class-modules/500/gradebook")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classModuleId").value(500))
                .andExpect(jsonPath("$.students").isArray())
                // Kiểm tra có sinh viên post_owner trong danh sách
                .andExpect(jsonPath("$.students[?(@.studentId == 1)].studentName").exists());
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void getModuleGradebook_WhenTeacherNotAuthorized_ShouldReturn403() throws Exception {
        // giaovien2 (id=6) không dạy module 500
        mockMvc.perform(get(BASE_URL + "/class-modules/500/gradebook"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getModuleGradebook_WhenUserIsStudent_ShouldReturn403() throws Exception {
        // Sinh viên không được xem sổ điểm cả lớp
        mockMvc.perform(get(BASE_URL + "/class-modules/500/gradebook"))
                .andExpect(status().isForbidden());
    }

    // --- TEST BATCH CREATE/UPDATE (Thay thế cho Create/Update đơn lẻ) ---

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void batchUpdateGrades_WhenTeacherIsAuthorized_ShouldReturn200() throws Exception {
        BatchGradeUpdateDTO batchDTO = new BatchGradeUpdateDTO();
        batchDTO.setClassModuleId(500L);
        batchDTO.setEntryDate(LocalDate.now());

        StudentScoreEntryDTO scoreEntry = new StudentScoreEntryDTO();
        scoreEntry.setStudentId(1L); // post_owner
        scoreEntry.setComponents(Map.of(
                "Điểm lý thuyết", new BigDecimal("9.5"),
                "Điểm thực hành", new BigDecimal("10.0")));

        batchDTO.setScores(List.of(scoreEntry));

        mockMvc.perform(post(BASE_URL + "/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "giaovien2", userDetailsServiceBeanName = "userService")
    void batchUpdateGrades_WhenTeacherNotAuthorized_ShouldReturn403() throws Exception {
        BatchGradeUpdateDTO batchDTO = new BatchGradeUpdateDTO();
        batchDTO.setClassModuleId(500L);
        batchDTO.setScores(List.of());

        mockMvc.perform(post(BASE_URL + "/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void batchUpdateGrades_WhenStudentNotFound_ShouldLogWarningButReturn200() throws Exception {
        // Logic service hiện tại log warn và bỏ qua student không tồn tại, không throw
        // error
        BatchGradeUpdateDTO batchDTO = new BatchGradeUpdateDTO();
        batchDTO.setClassModuleId(500L);

        StudentScoreEntryDTO scoreEntry = new StudentScoreEntryDTO();
        scoreEntry.setStudentId(999L); // ID không tồn tại
        scoreEntry.setComponents(Map.of("Điểm lý thuyết", BigDecimal.TEN));
        batchDTO.setScores(List.of(scoreEntry));

        mockMvc.perform(post(BASE_URL + "/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void batchUpdateGrades_WhenUnauthenticated_ShouldReturn401() throws Exception {
        BatchGradeUpdateDTO batchDTO = new BatchGradeUpdateDTO();
        mockMvc.perform(post(BASE_URL + "/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isUnauthorized());
    }

    // --- TEST GRADE HISTORY ---

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenStudentOwner_ShouldReturn200() throws Exception {
        // Grade ID 1 thuộc về post_owner
        mockMvc.perform(get(BASE_URL + "/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", instanceOf(List.class)));
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenAuthorizedTeacher_ShouldReturn200() throws Exception {
        // Giáo viên dạy lớp đó được xem history
        mockMvc.perform(get(BASE_URL + "/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", instanceOf(List.class)));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenOtherStudent_ShouldReturn403() throws Exception {
        // another_user (id=2) không được xem history của post_owner (id=1)
        mockMvc.perform(get(BASE_URL + "/1/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenGradeNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999/history"))
                .andExpect(status().isNotFound());
    }

    // --- TEST DELETE GRADE ---

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void deleteGrade_WhenTeacherIsAuthorized_ShouldReturn204() throws Exception {
        // Grade ID 2 thuộc về student 2 (another_user), module 500 -> GV1 có quyền xóa
        mockMvc.perform(delete(BASE_URL + "/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteGrade_WhenStudentTriesToDelete_ShouldReturn403() throws Exception {
        // Sinh viên không được xóa điểm
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isForbidden());
    }
}