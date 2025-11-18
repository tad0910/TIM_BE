package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.BatchGradeUpdateDTO;
import com.tim.appTim.dto.StudentScoreEntryDTO;
import com.tim.appTim.service.GradeService;
import com.tim.appTim.service.NotificationService;
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
import org.springframework.test.web.servlet.ResultActions;
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
@Sql("/test-data.sql") // Đảm bảo file này tạo dữ liệu khớp với logic (users, roles, classes)
@ActiveProfiles("test")
class GradeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private GradeService gradeService;

    @MockBean
    private NotificationService notificationService;

    private final String BASE_URL = "/grades";

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Mock notification để không bị lỗi khi service gọi nó
        doReturn(null).when(notificationService).createNotification(any(), any(), any(), any(), any(), any(), any());
    }

    // --- TEST KỊCH BẢN 1: SINH VIÊN XEM ĐIỂM ---

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService") // Giả sử đây là student 1
    void getMyGradesInModule_WhenStudentIsSelf_ShouldReturn200() throws Exception {
        // API: /grades/class-modules/{id}/my-grades
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isOk())
                // Kiểm tra cấu trúc DTO mới (không còn là List)
                .andExpect(jsonPath("$.studentId").exists())
                .andExpect(jsonPath("$.theoryScore").exists());
    }

    @Test
    @WithMockUser(username = "stranger_user", authorities = "ROLE_USER") // User không có trong lớp
    void getMyGradesInModule_WhenUserNotInClass_ShouldReturnForbidden() throws Exception {
        // Sẽ bị chặn bởi validateStudentMembership
        ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:read_detail") // Giả sử có quyền đọc
    void getModuleGradebook_WhenTeacherIsAuthorized_ShouldReturn200() throws Exception {
        // API: /grades/class-modules/{id}/gradebook
        mockMvc.perform(get(BASE_URL + "/class-modules/500/gradebook")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classModuleId").value(500))
                // Kiểm tra cấu trúc DTO mới
                .andExpect(jsonPath("$.students").isArray())
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    // --- TEST KỊCH BẢN 3: NHẬP ĐIỂM HÀNG LOẠT (MỚI) ---

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:create") // Cần quyền create
    void batchCreateOrUpdateGrades_WhenTeacherIsAuthorized_ShouldReturn200() throws Exception {
        // 1. Tạo DTO Batch
        BatchGradeUpdateDTO batchDTO = new BatchGradeUpdateDTO();
        batchDTO.setClassModuleId(500L);
        batchDTO.setEntryDate(LocalDate.now());

        // 2. Tạo 1 hàng điểm cho sinh viên (ID 1)
        StudentScoreEntryDTO studentScore = new StudentScoreEntryDTO();
        studentScore.setStudentId(1L);
        // Map điểm
        studentScore.setComponents(Map.of(
                "Điểm lý thuyết", new BigDecimal("8.5"),
                "Điểm thực hành", new BigDecimal("9.0")
        ));

        batchDTO.setScores(List.of(studentScore));

        // 3. Gọi API
        mockMvc.perform(post(BASE_URL + "/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "giaovien2", authorities = "grade:create") // GV không dạy lớp này
    void batchCreateOrUpdateGrades_WhenTeacherNotAuthorized_ShouldReturn403() throws Exception {
        BatchGradeUpdateDTO batchDTO = new BatchGradeUpdateDTO();
        batchDTO.setClassModuleId(500L);
        batchDTO.setScores(List.of()); // List rỗng cũng được, vì sẽ check quyền trước

        mockMvc.perform(post(BASE_URL + "/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchDTO)))
                .andExpect(status().isForbidden()); // Lỗi từ validateTeacherPermission
    }

    // --- TEST KỊCH BẢN 4: LỊCH SỬ ---

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenStudentOwner_ShouldReturn200() throws Exception {
        // Giả sử gradeId = 1 thuộc về post_owner
        mockMvc.perform(get(BASE_URL + "/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", instanceOf(Iterable.class)));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenOtherStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1/history"))
                .andExpect(status().isForbidden());
    }

    // --- TEST BẢO MẬT CHUNG ---

    @Test
    void getMyGradesInModule_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isUnauthorized());
    }
}