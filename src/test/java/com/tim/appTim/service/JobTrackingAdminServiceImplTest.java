package com.tim.appTim.service;

import com.tim.appTim.dto.AdminJobLeadDTO;
import com.tim.appTim.dto.JobActivityDTO;
import com.tim.appTim.dto.JobTrackingOverviewClassDTO;
import com.tim.appTim.dto.JobTrackingOverviewFilter;
import com.tim.appTim.dto.JobTrackingOverviewSummaryDTO;
import com.tim.appTim.dto.JobTrackingRowDTO;
import com.tim.appTim.dto.JobTrackingUpdateRequest;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.JobActivity;
import com.tim.appTim.entity.JobActivityType;
import com.tim.appTim.entity.JobApplication;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.JobActivityRepository;
import com.tim.appTim.repository.JobApplicationRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobTrackingAdminServiceImplTest {

    @Mock
    private ClassRepository classRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;
    @Mock
    private JobLeadRepository jobLeadRepository;
    @Mock
    private JobActivityRepository jobActivityRepository;
    @Mock
    private JobApplicationRepository jobApplicationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JobTrackingAdminServiceImpl jobTrackingAdminService;

    private Class classEntity;
    private ClassMember studentMember;
    private ClassMember teacherMember;
    private User student;
    private User teacher;
    private JobLead jobLead;
    private JobApplication jobApplication;
    private JobActivity jobActivity;

    @BeforeEach
    void setUp() {
        classEntity = new Class();
        classEntity.setId(1L);
        classEntity.setClassName("Test Class");
        classEntity.setProgramId(1);

        student = new User();
        student.setId(1L);
        student.setUsername("student1");
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setJobInterestEnabled(true);

        teacher = new User();
        teacher.setId(2L);
        teacher.setUsername("teacher1");

        studentMember = new ClassMember();
        studentMember.setId(1L);
        studentMember.setClassId(1L);
        studentMember.setUserId(1L);
        studentMember.setRole(ClassMember.Role.sinh_vien);
        studentMember.setUser(student);

        teacherMember = new ClassMember();
        teacherMember.setId(2L);
        teacherMember.setClassId(1L);
        teacherMember.setUserId(2L);
        teacherMember.setRole(ClassMember.Role.giao_vien);

        jobLead = new JobLead();
        jobLead.setId(1L);
        jobLead.setStudent(student);
        jobLead.setCompanyName("Test Company");
        jobLead.setStatus(JobLead.LeadStatus.NEW);
        jobLead.setCreatedAt(LocalDateTime.now().minusDays(5));
        jobLead.setCreatedByAdmin(false);

        jobApplication = new JobApplication();
        jobApplication.setId(1L);
        jobApplication.setStudent(student);
        jobApplication.setStatus(JobApplication.ApplicationStatus.SENT_CV);
        jobApplication.setAppliedAt(LocalDateTime.now().minusDays(3));

        jobActivity = new JobActivity();
        jobActivity.setId(1L);
        jobActivity.setJobLeadId(1L);
        jobActivity.setActivityType(JobActivityType.OFFER_RECEIVED);
        jobActivity.setContent("Received offer");
        jobActivity.setSalaryAmount("1000");
        jobActivity.setCreatedAt(LocalDateTime.now().minusDays(1));
        jobActivity.setHappenedAt(LocalDateTime.now().minusDays(1).toLocalDate());
    }

    @Test
    void getJobTrackingByClass_Success() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo(1L);
        assertThat(result.get(0).getStudentName()).isEqualTo("John Doe");
        assertThat(result.get(0).getCompanyName()).isEqualTo("Test Company");
        assertThat(result.get(0).isJobInterest()).isTrue();
    }

    @Test
    void getJobTrackingByClass_ClassNotFound_ShouldThrowException() {
        when(classRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.getJobTrackingByClass(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lớp học");
    }

    @Test
    void getJobTrackingByClass_NoStudents_ShouldReturnEmptyList() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getJobTrackingByClass_FiltersOutNonStudents() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember, teacherMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo(1L);
    }

    @Test
    void getJobTrackingByClass_WithActivities_ShouldIncludeSalaryInfo() {
        JobActivity probationActivity = new JobActivity();
        probationActivity.setId(2L);
        probationActivity.setJobLeadId(1L);
        probationActivity.setActivityType(JobActivityType.PROBATION_CONTRACT);
        probationActivity.setSalaryAmount("800");
        probationActivity.setCreatedAt(LocalDateTime.now().minusDays(2));

        JobActivity officialActivity = new JobActivity();
        officialActivity.setId(3L);
        officialActivity.setJobLeadId(1L);
        officialActivity.setActivityType(JobActivityType.OFFICIAL_CONTRACT);
        officialActivity.setSalaryAmount("1200");
        officialActivity.setCreatedAt(LocalDateTime.now());

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(officialActivity, probationActivity, jobActivity));

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOfferAmount()).isEqualTo("1000");
        assertThat(result.get(0).getProbationSalary()).isEqualTo("800");
        assertThat(result.get(0).getOfficialSalary()).isEqualTo("1200");
    }

    @Test
    void getJobTrackingByClass_WithFallbackApplication() {
        jobApplication.setStatus(JobApplication.ApplicationStatus.SENT_CV);
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(List.of(jobApplication));
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobStatusCode()).isEqualTo("SENT_CV");
    }

    @Test
    void getJobTrackingByClass_WithLatestActivity() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(jobActivity));

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastUpdated()).isNotNull();
    }

    @Test
    void getJobTrackingByClass_StudentNameFromUsername() {
        // Create a new student with empty firstName and lastName but with username
        User studentWithUsername = new User();
        studentWithUsername.setId(1L);
        studentWithUsername.setFirstName("");
        studentWithUsername.setLastName("");
        studentWithUsername.setUsername("student1");
        
        studentMember.setUser(null);

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(userRepository.findById(1L)).thenReturn(Optional.of(studentWithUsername));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentName()).isEqualTo("student1");
    }

    @Test
    void updateJobInterest_Success() {
        JobTrackingUpdateRequest request = new JobTrackingUpdateRequest();
        request.setJobInterest(false);

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.of(studentMember));
        when(jobLeadRepository.findTopByStudentIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(jobLead));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        JobTrackingRowDTO result = jobTrackingAdminService.updateJobInterest(1L, 1L, request);

        assertThat(result).isNotNull();
        assertThat(result.isJobInterest()).isFalse();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateJobInterest_ClassNotFound_ShouldThrowException() {
        JobTrackingUpdateRequest request = new JobTrackingUpdateRequest();
        when(classRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.updateJobInterest(1L, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lớp học");
    }

    @Test
    void updateJobInterest_MemberNotFound_ShouldThrowException() {
        JobTrackingUpdateRequest request = new JobTrackingUpdateRequest();
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.updateJobInterest(1L, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Học viên không thuộc lớp này");
    }

    @Test
    void updateJobInterest_NoLead_Success() {
        JobTrackingUpdateRequest request = new JobTrackingUpdateRequest();
        request.setJobInterest(true);

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.of(studentMember));
        when(jobLeadRepository.findTopByStudentIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.save(any(User.class))).thenReturn(student);
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingRowDTO result = jobTrackingAdminService.updateJobInterest(1L, 1L, request);

        assertThat(result).isNotNull();
        assertThat(result.isJobInterest()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(jobLeadRepository, never()).save(any(JobLead.class));
    }

    @Test
    void getJobTrackingOverview_Success() {
        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result).isNotNull();
        assertThat(result.getTotalClasses()).isEqualTo(1);
        assertThat(result.getTotalStudents()).isEqualTo(1);
    }

    @Test
    void getJobTrackingOverview_WithNullFilter() {
        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(null);

        assertThat(result).isNotNull();
    }

    @Test
    void getJobTrackingOverview_WithProgramFilter() {
        Class class2 = new Class();
        class2.setId(2L);
        class2.setProgramId(2);

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        filter.setProgramId(1);

        when(classRepository.findAll()).thenReturn(List.of(classEntity, class2));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getTotalClasses()).isEqualTo(1);
    }

    @Test
    void getJobTrackingOverview_WithMentorFilter() {
        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        filter.setMentorId(2L);

        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.existsByClassIdAndUserIdAndRole(1L, 2L, ClassMember.Role.giao_vien))
                .thenReturn(true);
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getTotalClasses()).isEqualTo(1);
    }

    @Test
    void getJobTrackingOverview_WithMentorFilter_NoMatch() {
        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        filter.setMentorId(2L);

        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classMemberRepository.existsByClassIdAndUserIdAndRole(1L, 2L, ClassMember.Role.giao_vien))
                .thenReturn(false);

        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getTotalClasses()).isEqualTo(0);
    }

    @Test
    void getJobTrackingOverview_CountsOffers() {
        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        jobLead.setStatus(JobLead.LeadStatus.OFFER);

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getTotalOffers()).isEqualTo(1);
    }

    @Test
    void getJobTrackingOverview_CountsRecentUpdates() {
        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of(jobActivity));

        jobActivity.setCreatedAt(LocalDateTime.now().minusDays(5));

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getUpdatedWithin14Days()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getJobTrackingOverview_CalculatesRecentPercent() {
        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getRecentUpdatePercent()).isGreaterThanOrEqualTo(0);
        assertThat(result.getRecentUpdatePercent()).isLessThanOrEqualTo(100);
    }

    @Test
    void getStudentLeads_Success() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.of(studentMember));
        when(jobLeadRepository.findByStudentIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(jobLead));
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        List<AdminJobLeadDTO> result = jobTrackingAdminService.getStudentLeads(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Test Company");
        assertThat(result.get(0).getStatusCode()).isEqualTo("NEW");
    }

    @Test
    void getStudentLeads_ClassNotFound_ShouldThrowException() {
        when(classRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.getStudentLeads(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lớp học");
    }

    @Test
    void getStudentLeads_MemberNotFound_ShouldThrowException() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.getStudentLeads(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Học viên không thuộc lớp này");
    }

    @Test
    void getStudentLeads_WithActivities() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.of(studentMember));
        when(jobLeadRepository.findByStudentIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(jobLead));
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(jobActivity));

        List<AdminJobLeadDTO> result = jobTrackingAdminService.getStudentLeads(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivities()).hasSize(1);
        assertThat(result.get(0).getActivities().get(0).getActivityType()).isEqualTo("OFFER_RECEIVED");
    }

    @Test
    void getStudentLeads_WithNullStatus() {
        jobLead.setStatus(null);

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.of(studentMember));
        when(jobLeadRepository.findByStudentIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(jobLead));
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        List<AdminJobLeadDTO> result = jobTrackingAdminService.getStudentLeads(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatusCode()).isNull();
        assertThat(result.get(0).getStatusLabel()).isEqualTo("Chưa có trạng thái");
    }

    @Test
    void createJobLead_Success() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.of(studentMember));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(jobLeadRepository.save(any(JobLead.class))).thenAnswer(invocation -> {
            JobLead lead = invocation.getArgument(0);
            lead.setId(2L);
            return lead;
        });
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        AdminJobLeadDTO result = jobTrackingAdminService.createJobLead(1L, 1L, "New Company", "NC", "Address", "Website");

        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo("New Company");
        assertThat(result.getShortName()).isEqualTo("NC");
        assertThat(result.getAddress()).isEqualTo("Address");
        assertThat(result.getWebsite()).isEqualTo("Website");
        assertThat(result.getStatusCode()).isEqualTo("NEW");
        assertThat(result.isFromAdmin()).isTrue();
        verify(jobLeadRepository).save(any(JobLead.class));
    }

    @Test
    void createJobLead_ClassNotFound_ShouldThrowException() {
        when(classRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.createJobLead(1L, 1L, "Company", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lớp học");
    }

    @Test
    void createJobLead_MemberNotFound_ShouldThrowException() {
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.createJobLead(1L, 1L, "Company", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Học viên không thuộc lớp này");
    }

    @Test
    void createJobLead_StudentNotFound_ShouldThrowException() {
        studentMember.setUser(null);
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassIdAndUserId(1L, 1L)).thenReturn(Optional.of(studentMember));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobTrackingAdminService.createJobLead(1L, 1L, "Company", null, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy thông tin học viên");
    }

    @Test
    void getJobTrackingByClass_WithMultipleLeads_SelectsLatest() {
        JobLead olderLead = new JobLead();
        olderLead.setId(2L);
        olderLead.setStudent(student);
        olderLead.setCompanyName("Older Company");
        olderLead.setCreatedAt(LocalDateTime.now().minusDays(10));

        JobLead newerLead = new JobLead();
        newerLead.setId(3L);
        newerLead.setStudent(student);
        newerLead.setCompanyName("Newer Company");
        newerLead.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(olderLead, newerLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Newer Company");
    }

    @Test
    void getJobTrackingByClass_WithNullStudentInLead_ShouldSkip() {
        JobLead leadWithNullStudent = new JobLead();
        leadWithNullStudent.setId(2L);
        leadWithNullStudent.setStudent(null);
        leadWithNullStudent.setCompanyName("Test");

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(leadWithNullStudent));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("-");
    }

    @Test
    void getJobTrackingByClass_WithBestApplication() {
        JobApplication app1 = new JobApplication();
        app1.setId(1L);
        app1.setStudent(student);
        app1.setStatus(JobApplication.ApplicationStatus.SENT_CV);
        app1.setAppliedAt(LocalDateTime.now().minusDays(5));

        JobApplication app2 = new JobApplication();
        app2.setId(2L);
        app2.setStudent(student);
        app2.setStatus(JobApplication.ApplicationStatus.INTERVIEW);
        app2.setAppliedAt(LocalDateTime.now().minusDays(1));

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(List.of(app1, app2));
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastUpdated()).isEqualTo(app2.getAppliedAt());
    }

    @Test
    void getJobTrackingByClass_WithNullActivityType_ShouldSkip() {
        JobActivity nullTypeActivity = new JobActivity();
        nullTypeActivity.setId(2L);
        nullTypeActivity.setJobLeadId(1L);
        nullTypeActivity.setActivityType(null);
        nullTypeActivity.setCreatedAt(LocalDateTime.now());

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(nullTypeActivity, jobActivity));

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOfferAmount()).isEqualTo("1000");
    }

    @Test
    void getJobTrackingByClass_WithActivityButNoLead() {
        // Create a lead with activity but don't include it in the leads list
        JobLead leadWithActivity = new JobLead();
        leadWithActivity.setId(2L);
        leadWithActivity.setStudent(student);
        leadWithActivity.setCompanyName("Test Company");
        leadWithActivity.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobStatusCode()).isEqualTo("NONE");
    }

    @Test
    void getJobTrackingByClass_WithBlankSalary_ShouldNotUpdate() {
        JobActivity blankSalaryActivity = new JobActivity();
        blankSalaryActivity.setId(2L);
        blankSalaryActivity.setJobLeadId(1L);
        blankSalaryActivity.setActivityType(JobActivityType.OFFER_RECEIVED);
        blankSalaryActivity.setSalaryAmount("   ");
        blankSalaryActivity.setCreatedAt(LocalDateTime.now());

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(blankSalaryActivity));

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOfferAmount()).isEqualTo("-");
    }

    @Test
    void getJobTrackingByClass_UserNotFound_ShouldHandleGracefully() {
        studentMember.setUser(null);
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentName()).isEqualTo("");
    }

    @Test
    void getJobTrackingByClass_WithApplicationCompany() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Application Company");
        jobApplication.setCompany(company);
        jobApplication.setStatus(JobApplication.ApplicationStatus.SENT_CV);

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(List.of(jobApplication));
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Application Company");
    }

    @Test
    void getJobTrackingByClass_WithApplicationNullCompany() {
        jobApplication.setCompany(null);
        jobApplication.setStatus(JobApplication.ApplicationStatus.SENT_CV);

        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(List.of(jobApplication));
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        List<JobTrackingRowDTO> result = jobTrackingAdminService.getJobTrackingByClass(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("-");
    }

    @Test
    void getJobTrackingOverview_WithEmptyClasses() {
        when(classRepository.findAll()).thenReturn(Collections.emptyList());

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getTotalClasses()).isEqualTo(0);
        assertThat(result.getTotalStudents()).isEqualTo(0);
    }

    @Test
    void getJobTrackingOverview_WithNullAppliedAt() {
        jobApplication.setAppliedAt(null);
        jobApplication.setStatus(JobApplication.ApplicationStatus.SENT_CV);

        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(List.of(jobApplication));
        lenient().when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result).isNotNull();
    }

    @Test
    void getJobTrackingOverview_WithOfficialStatus() {
        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        jobLead.setStatus(JobLead.LeadStatus.OFFICIAL);

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getTotalOffers()).isEqualTo(1);
    }

    @Test
    void getJobTrackingOverview_WithProgramName() {
        Programs program = new Programs();
        program.setId(1);
        program.setName("Test Program");
        classEntity.setProgram(program);

        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getClasses()).hasSize(1);
        assertThat(result.getClasses().get(0).getProgramName()).isEqualTo("Test Program");
    }

    @Test
    void getJobTrackingOverview_WithNullProgram() {
        classEntity.setProgram(null);

        when(classRepository.findAll()).thenReturn(List.of(classEntity));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(classMemberRepository.findByClassId(1L)).thenReturn(List.of(studentMember));
        when(jobLeadRepository.findByStudentIdIn(anyList())).thenReturn(List.of(jobLead));
        when(jobApplicationRepository.findByClassEntityId(1L)).thenReturn(Collections.emptyList());
        when(jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(anyLong())).thenReturn(Collections.emptyList());

        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        JobTrackingOverviewSummaryDTO result = jobTrackingAdminService.getJobTrackingOverview(filter);

        assertThat(result.getClasses()).hasSize(1);
        assertThat(result.getClasses().get(0).getProgramName()).isNull();
    }
}

