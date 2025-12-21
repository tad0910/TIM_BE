package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.ApprovalRequestDTO;
import com.tim.appTim.dto.StudentFormCreateDTO;
import com.tim.appTim.entity.StudentForm;
import com.tim.appTim.repository.StudentFormRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
class StudentFormIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentFormRepository studentFormRepository;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("GET /forms/templates trả về danh sách mẫu active")
    @WithUserDetails(value = "form_admin", userDetailsServiceBeanName = "userService")
    void getTemplates_ShouldReturnActiveTemplates() throws Exception {
        mockMvc.perform(get("/forms/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].code").value(containsInAnyOrder("RESERVATION", "TRANSFER")));
    }

    @Test
    @DisplayName("POST /forms tạo đơn hợp lệ")
    @WithUserDetails(value = "form_admin", userDetailsServiceBeanName = "userService")
    void createForm_ShouldReturn200_WhenPayloadValid() throws Exception {
        StudentFormCreateDTO dto = buildValidFormCreateDTO();

        mockMvc.perform(post("/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(8))
                .andExpect(jsonPath("$.classId").value(10))
                .andExpect(jsonPath("$.templateName").value("Đơn Bảo lưu"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /forms trả 400 khi tên học viên không khớp")
    @WithUserDetails(value = "form_admin", userDetailsServiceBeanName = "userService")
    void createForm_ShouldReturn400_WhenFullNameMismatch() throws Exception {
        StudentFormCreateDTO dto = buildValidFormCreateDTO();
        dto.setFullName("Tên Sai");

        mockMvc.perform(post("/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /forms/{id}/approve cập nhật trạng thái Coach khi GV duyệt")
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void approveForm_ShouldUpdateCoachApproval() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO();
        request.setDecision(StudentForm.ApprovalStatus.APPROVED);
        request.setNote("Đã review");

        mockMvc.perform(put("/forms/2000/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2000))
                .andExpect(jsonPath("$.coachApproval").value("APPROVED"))
                .andExpect(jsonPath("$.coachNote").value("Đã review"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("PUT /forms/{id}/approve trả 403 khi GV duyệt đơn không thuộc lớp mình")
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void approveForm_ShouldReturn403_WhenTeacherApprovesOtherClassForm() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO();
        request.setDecision(StudentForm.ApprovalStatus.APPROVED);
        request.setNote("Không được phép");

        mockMvc.perform(put("/forms/2001/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /forms/{id}/approve trả 403 khi non-admin cố duyệt/note thay role khác")
    @WithUserDetails(value = "giaovu_user", userDetailsServiceBeanName = "userService")
    void approveForm_ShouldReturn403_WhenNonAdminUsesDifferentTargetRole() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO();
        request.setDecision(StudentForm.ApprovalStatus.APPROVED);
        request.setNote("Cố tình duyệt thay");
        request.setTargetRole("ROLE_ADMIN");

        mockMvc.perform(put("/forms/2000/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /forms/{id}/approve cho phép Admin duyệt/note thay ROLE_GIAO_VIEN")
    @WithUserDetails(value = "form_admin", userDetailsServiceBeanName = "userService")
    void approveForm_ShouldAllowAdminApproveOnBehalfOfTeacher() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO();
        request.setDecision(StudentForm.ApprovalStatus.APPROVED);
        request.setNote("Admin duyệt thay GV");
        request.setTargetRole("ROLE_GIAO_VIEN");

        mockMvc.perform(put("/forms/2000/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2000))
                .andExpect(jsonPath("$.coachApproval").value("APPROVED"))
                .andExpect(jsonPath("$.coachNote").value("Admin duyệt thay GV"));
    }

    @Test
    @DisplayName("PUT /forms/{id}/approve trả 403 khi user không có quyền")
    @WithUserDetails(value = "form_student", userDetailsServiceBeanName = "userService")
    void approveForm_ShouldReturn403_WhenUserLacksPermission() throws Exception {
        ApprovalRequestDTO request = new ApprovalRequestDTO();
        request.setDecision(StudentForm.ApprovalStatus.APPROVED);

        mockMvc.perform(put("/forms/2000/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /forms/{id} trả về chi tiết đơn")
    @WithUserDetails(value = "form_admin", userDetailsServiceBeanName = "userService")
    void getFormDetail_ShouldReturnForm() throws Exception {
        mockMvc.perform(get("/forms/2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2000))
                .andExpect(jsonPath("$.studentId").value(8))
                .andExpect(jsonPath("$.classId").value(10));
    }

    @Test
    @DisplayName("GET /forms trả về danh sách phù hợp với giáo viên")
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getAllForms_ShouldFilterByTeacherClasses() throws Exception {
        mockMvc.perform(get("/forms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].classId").value(10));
    }

    @Test
    @DisplayName("DELETE /forms/{id} xóa đơn khi Admin có quyền")
    @WithUserDetails(value = "form_admin", userDetailsServiceBeanName = "userService")
    void deleteForm_ShouldRemoveRecord() throws Exception {
        mockMvc.perform(delete("/forms/2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Đã xóa đơn thành công"));

        assertFalse(studentFormRepository.existsById(2001L), "Form phải bị xóa khỏi DB");
    }

    private StudentFormCreateDTO buildValidFormCreateDTO() {
        StudentFormCreateDTO dto = new StudentFormCreateDTO();
        dto.setTemplateId(1000L);
        dto.setStudentId(8L);
        dto.setClassId(10L);
        dto.setFullName("Form Student");
        dto.setReason("Xin phép bảo lưu do lý do cá nhân");
        dto.setStartDate(LocalDate.of(2025, 5, 1));
        dto.setEndDate(LocalDate.of(2025, 5, 31));
        dto.setFeeAmount(BigDecimal.ZERO);
        dto.setPhoneNumber("0911000008");
        dto.setEmail("form.student@example.com");
        return dto;
    }
}

