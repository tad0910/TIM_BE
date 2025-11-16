package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.GradeCreateDTO;
import com.tim.appTim.dto.GradeUpdateDTO;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

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

        doReturn(null).when(notificationService).createNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "post_owner", authorities = "grade:read_all")
    void getMyGradesInModule_WhenStudentIsSelf_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", instanceOf(Iterable.class)))
                .andExpect(jsonPath("$[0].componentName").value("Bài tập 1"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyGradesInModule_WhenMissingAuthority_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "another_user", authorities = "grade:read_all")
    void getMyGradesInModule_WhenOtherStudentInSameClass_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "giaovien3", authorities = "grade:read_all")
    void getMyGradesInModule_WhenUserNotInClass_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:read_detail")
    void getStudentGradesInModule_WhenTeacherIsAuthorized_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/grades")
                        .param("studentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", instanceOf(Iterable.class)));
    }

    @Test
    @WithMockUser(username = "giaovien2", authorities = "grade:read_detail")
    void getStudentGradesInModule_WhenTeacherNotAuthorized_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/grades")
                        .param("studentId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getStudentGradesInModule_WhenUserIsStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/grades")
                        .param("studentId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:read_detail")
    void getModuleGradebook_WhenTeacherIsAuthorized_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/gradebook")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classModuleId").value(500))
                .andExpect(jsonPath("$.students").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:update")
    void updateGrade_WhenTeacherIsAuthorized_ShouldReturn200() throws Exception {
        GradeUpdateDTO updateDTO = new GradeUpdateDTO();
        updateDTO.setNewScore(new BigDecimal("9.5"));
        updateDTO.setChangeReason("Chấm lại bài");

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(9.5));
    }

    @Test
    @WithMockUser(username = "giaovien2", authorities = "grade:update")
    void updateGrade_WhenTeacherNotAuthorized_ShouldReturn403() throws Exception {
        GradeUpdateDTO updateDTO = new GradeUpdateDTO();
        updateDTO.setNewScore(new BigDecimal("9.5"));
        updateDTO.setChangeReason("Chấm lại bài");

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:create")
    void createGrade_WhenTeacherIsAuthorized_ShouldReturn201() throws Exception {
        GradeCreateDTO createDTO = new GradeCreateDTO();
        createDTO.setClassModuleId(500L);
        createDTO.setStudentId(2L);
        createDTO.setComponentName("Thi cuối kỳ");
        createDTO.setScore(new BigDecimal("9.0"));
        createDTO.setMaxScore(new BigDecimal("10.0"));
        createDTO.setWeightPercent(new BigDecimal("50"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.componentName").value("Thi cuối kỳ"))
                .andExpect(jsonPath("$.score").value(9.0));
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:create")
    void createGrade_WhenStudentNotFound_ShouldReturn404() throws Exception {
        GradeCreateDTO createDTO = new GradeCreateDTO();
        createDTO.setClassModuleId(500L);
        createDTO.setStudentId(999L);
        createDTO.setComponentName("Thi cuối kỳ");
        createDTO.setScore(new BigDecimal("9.0"));
        createDTO.setMaxScore(new BigDecimal("10.0"));
        createDTO.setWeightPercent(new BigDecimal("50"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenStudentOwner_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", instanceOf(Iterable.class)));
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenAuthorizedTeacher_ShouldReturn200() throws Exception {
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

    @Test
    void getMyGradesInModule_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/my-grades"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getModuleGradebook_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/500/gradebook"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateGrade_WhenUnauthenticated_ShouldReturn401() throws Exception {
        String updateJson = """
            { "newScore": 10.0, "changeReason": "test" }
        """;

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGrade_WhenUnauthenticated_ShouldReturn401() throws Exception {
        String createJson = """
            { "classModuleId": 500, "studentId": 1, "componentName": "Test", "score": 8.0 }
        """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:update")
    void updateGrade_WhenGradeNotFound_ShouldReturn404() throws Exception {
        GradeUpdateDTO updateDTO = new GradeUpdateDTO();
        updateDTO.setNewScore(new BigDecimal("9.0"));
        updateDTO.setChangeReason("Test 404");

        mockMvc.perform(put(BASE_URL + "/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getGradeHistory_WhenGradeNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999/history"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:read_detail")
    void getModuleGradebook_WhenClassModuleNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/class-modules/9999/gradebook"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:create")
    void createGrade_WhenBodyIsInvalid_ShouldReturn400() throws Exception {
        String invalidJson = """
            {
                "classModuleId": 500,
                "componentName": "Thiếu thông tin"
            }
        """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "giaovien1", authorities = "grade:update")
    void updateGrade_WhenBodyIsInvalid_ShouldReturn400() throws Exception {
        String invalidJson = """
            {
                "changeReason": "Thiếu điểm mới"
            }
        """;

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
