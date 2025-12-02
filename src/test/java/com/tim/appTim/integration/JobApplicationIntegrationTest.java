package com.tim.appTim.integration;

import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.JobApplication;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.CompanyRepository;
import com.tim.appTim.repository.JobApplicationRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.service.FileUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class JobApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @MockBean
    private FileUploadService fileUploadService;

    @Test
    @WithMockUser(username = "admin", authorities = { "job:create" })
    void testIntroduceStudentsToCompany_ShouldReturn200() throws Exception {
        // Setup
        Company company = new Company();
        company.setName("Intro Company");
        company.setShortName("IC");
        company.setAddress("Address");
        company.setWebsite("site.com");
        company = companyRepository.save(company);

        // Use existing class 10 and student 1 from test-data.sql
        Long classId = 10L;
        Long studentId = 1L;

        MockMultipartFile cvFile = new MockMultipartFile("cvFiles", "cv.pdf", "application/pdf", "content".getBytes());

        when(fileUploadService.uploadFile(any())).thenReturn("http://cloudinary.com/cv.pdf");

        mockMvc.perform(multipart("/api/admin/job-applications/introduce")
                .file(cvFile)
                .param("classId", String.valueOf(classId))
                .param("companyId", String.valueOf(company.getId()))
                .param("studentIds", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(content().string("Giới thiệu thành công!"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = { "job:read" })
    void testGetJobTrackingList_ShouldReturnList() throws Exception {
        // Setup
        Company company = new Company();
        company.setName("Tracking Company");
        company.setShortName("TC");
        company.setAddress("Address");
        company.setWebsite("site.com");
        company = companyRepository.save(company);

        Class classEntity = classRepository.findById(10L).orElseThrow();
        User student = userRepository.findById(1L).orElseThrow();

        JobApplication app = new JobApplication();
        app.setStudent(student);
        app.setCompany(company);
        app.setClassEntity(classEntity);
        app.setStatus(JobApplication.ApplicationStatus.SENT_CV);
        app.setAppliedAt(LocalDateTime.now());
        jobApplicationRepository.save(app);

        mockMvc.perform(get("/api/admin/job-applications/tracking/" + classEntity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // Check if at least one student has the company name
                .andExpect(jsonPath("$[?(@.studentId == 1)].companyName").value("Tracking Company"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = { "job:create" })
    void testIntroduce_WhenStudentCountMismatch_ShouldReturn400() throws Exception {
        Long classId = 10L;
        Long studentId = 1L;
        Long companyId = 1L; // Assuming exists or mocked

        // 1 student, 0 CVs (if required? Controller says: if cvFiles != null && size
        // mismatch)
        // Let's send 1 student and 2 CVs
        MockMultipartFile cvFile1 = new MockMultipartFile("cvFiles", "cv1.pdf", "application/pdf",
                "content".getBytes());
        MockMultipartFile cvFile2 = new MockMultipartFile("cvFiles", "cv2.pdf", "application/pdf",
                "content".getBytes());

        mockMvc.perform(multipart("/api/admin/job-applications/introduce")
                .file(cvFile1)
                .file(cvFile2)
                .param("classId", String.valueOf(classId))
                .param("companyId", String.valueOf(companyId))
                .param("studentIds", String.valueOf(studentId)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng học viên và số lượng CV không khớp!"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = { "job:create" })
    void testIntroduce_WhenCompanyNotFound_ShouldReturn404() throws Exception {
        Long classId = 10L;
        Long studentId = 1L;
        Long nonExistentCompanyId = 9999L;

        mockMvc.perform(multipart("/api/admin/job-applications/introduce")
                .param("classId", String.valueOf(classId))
                .param("companyId", String.valueOf(nonExistentCompanyId))
                .param("studentIds", String.valueOf(studentId)))
                .andExpect(status().isNotFound());
    }
}
