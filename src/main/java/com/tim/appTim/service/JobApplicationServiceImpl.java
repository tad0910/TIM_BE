package com.tim.appTim.service;

import com.tim.appTim.dto.StudentJobTrackingDTO;
import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.JobApplication;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.CompanyRepository;
import com.tim.appTim.repository.JobApplicationRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    private final ClassRepository classRepository;
    private final ClassService classService;

    @Override
    @Transactional
    public void introduceBatch(Long classId, Long companyId, List<Long> studentIds, List<MultipartFile> cvFiles) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Công ty không tồn tại"));

        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học không tồn tại ID: " + classId));

        List<JobApplication> applications = new ArrayList<>();

        for (int i = 0; i < studentIds.size(); i++) {
            Long studentId = studentIds.get(i);

            MultipartFile cvFile = (cvFiles != null && i < cvFiles.size()) ? cvFiles.get(i) : null;

            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Học viên ID " + studentId + " không tồn tại"));

            JobApplication app = new JobApplication();
            app.setStudent(student);
            app.setCompany(company);
            app.setClassEntity(classEntity);

            app.setStatus(JobApplication.ApplicationStatus.SENT_CV);
            app.setAppliedAt(LocalDateTime.now());

            if (cvFile != null && !cvFile.isEmpty()) {
                try {
                    String cvUrl = fileUploadService.uploadFile(cvFile);
                    app.setCvUrl(cvUrl);
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi upload CV cho học viên: " + student.getLastName());
                }
            }

            applications.add(app);
        }

        jobApplicationRepository.saveAll(applications);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentJobTrackingDTO> getJobTrackingList(Long classId) {
        List<ClassMember> members = classService.getClassMembersByClassId(classId);

        List<JobApplication> applications = jobApplicationRepository.findByClassEntityId(classId);

        Map<Long, List<JobApplication>> appMap = applications.stream()
                .collect(Collectors.groupingBy(app -> app.getStudent().getId()));

        List<StudentJobTrackingDTO> trackingList = new ArrayList<>();

        for (ClassMember member : members) {
            User student = member.getUser();

            if (student == null) continue;

            StudentJobTrackingDTO dto = new StudentJobTrackingDTO();
            dto.setStudentId(student.getId());
            dto.setStudentName(student.getUsername());
            dto.setJobSeeking(true);
            List<JobApplication> studentApps = appMap.get(student.getId());

            if (studentApps == null || studentApps.isEmpty()) {
                dto.setJobStatus("Chưa làm gì");
                dto.setCompanyName("-");
                dto.setOfferAmount("-");
                dto.setProbationSalary("-");
                dto.setOfficialSalary("-");
            } else {
                JobApplication bestApp = getBestApplication(studentApps);

                dto.setJobStatus(bestApp.getStatus().getDisplayName());
                dto.setCompanyName(bestApp.getCompany().getName());
                dto.setOfferAmount("-");
                dto.setProbationSalary("-");
                dto.setOfficialSalary("-");
            }
            trackingList.add(dto);
        }

        return trackingList;
    }

    private JobApplication getBestApplication(List<JobApplication> apps) {
        return apps.stream()
                .max((a1, a2) -> a1.getAppliedAt().compareTo(a2.getAppliedAt()))
                .orElse(apps.get(0));
    }
}