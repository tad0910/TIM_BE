package com.tim.appTim.service;

import com.tim.appTim.dto.StudentJobTrackingDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.JobApplication;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.CompanyRepository;
import com.tim.appTim.repository.JobApplicationRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private ClassService classService;

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    private Class classEntity;
    private Company company;
    private User student;
    private MultipartFile cvFile;

    @BeforeEach
    void setUp() {
        classEntity = new Class();
        classEntity.setId(1L);
        classEntity.setClassName("Class A");

        company = new Company();
        company.setId(1L);
        company.setName("Company A");

        student = new User();
        student.setId(1L);
        student.setUsername("student1");
        student.setLastName("Nguyen");

        cvFile = mock(MultipartFile.class);
    }

    @Test
    void introduceBatch_Success() throws IOException {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cvFile.isEmpty()).thenReturn(false);
        when(fileUploadService.uploadFile(cvFile)).thenReturn("http://cv.url");

        jobApplicationService.introduceBatch(1L, 1L, List.of(1L), List.of(cvFile));

        verify(jobApplicationRepository).saveAll(anyList());
        verify(fileUploadService).uploadFile(cvFile);
    }

    @Test
    void introduceBatch_CompanyNotFound_ShouldThrowException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobApplicationService.introduceBatch(1L, 1L, List.of(1L), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Công ty không tồn tại");
    }

    @Test
    void introduceBatch_ClassNotFound_ShouldThrowException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(classRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobApplicationService.introduceBatch(1L, 1L, List.of(1L), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lớp học không tồn tại");
    }

    @Test
    void introduceBatch_StudentNotFound_ShouldThrowException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobApplicationService.introduceBatch(1L, 1L, List.of(1L), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Học viên ID 1 không tồn tại");
    }

    @Test
    void introduceBatch_FileUploadError_ShouldThrowException() throws IOException {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cvFile.isEmpty()).thenReturn(false);
        when(fileUploadService.uploadFile(cvFile)).thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> jobApplicationService.introduceBatch(1L, 1L, List.of(1L), List.of(cvFile)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lỗi upload CV cho học viên");
    }

    @Test
    void getJobTrackingList_Success_WithApplications() {
        ClassMember member = new ClassMember();
        member.setUser(student);

        JobApplication app = new JobApplication();
        app.setStudent(student);
        app.setCompany(company);
        app.setStatus(JobApplication.ApplicationStatus.SENT_CV);
        app.setAppliedAt(LocalDateTime.now());

        when(classService.getClassMembersByClassId(1L)).thenReturn(List.of(member));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(List.of(app));

        List<StudentJobTrackingDTO> result = jobApplicationService.getJobTrackingList(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo(1L);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Company A");
        assertThat(result.get(0).getJobStatus()).isEqualTo("Gửi CV");
    }

    @Test
    void getJobTrackingList_Success_NoApplications() {
        ClassMember member = new ClassMember();
        member.setUser(student);

        when(classService.getClassMembersByClassId(1L)).thenReturn(List.of(member));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());

        List<StudentJobTrackingDTO> result = jobApplicationService.getJobTrackingList(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo(1L);
        assertThat(result.get(0).getJobStatus()).isEqualTo("Chưa làm gì");
        assertThat(result.get(0).getCompanyName()).isEqualTo("-");
    }

    @Test
    void getJobTrackingList_Success_MultipleApps_ShouldReturnBest() {
        ClassMember member = new ClassMember();
        member.setUser(student);

        JobApplication app1 = new JobApplication();
        app1.setStudent(student);
        app1.setCompany(company);
        app1.setStatus(JobApplication.ApplicationStatus.SENT_CV);
        app1.setAppliedAt(LocalDateTime.now().minusDays(1));

        JobApplication app2 = new JobApplication();
        app2.setStudent(student);
        app2.setCompany(company);
        app2.setStatus(JobApplication.ApplicationStatus.INTERVIEW);
        app2.setAppliedAt(LocalDateTime.now());

        when(classService.getClassMembersByClassId(1L)).thenReturn(List.of(member));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(List.of(app1, app2));

        List<StudentJobTrackingDTO> result = jobApplicationService.getJobTrackingList(1L);

        assertThat(result).hasSize(1);
        // Assuming INTERVIEW is "better" or just latest based on logic.
        // Logic is max by appliedAt.
        assertThat(result.get(0).getJobStatus()).isEqualTo("Phỏng vấn");
    }
}
