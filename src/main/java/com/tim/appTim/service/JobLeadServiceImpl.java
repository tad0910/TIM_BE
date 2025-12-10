package com.tim.appTim.service;

import com.tim.appTim.dto.JobLeadDTO;
import com.tim.appTim.dto.NotificationDTO;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.JobApplication;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.JobApplicationRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import com.tim.appTim.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class JobLeadServiceImpl implements JobLeadService {

    private final JobLeadRepository jobLeadRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassModuleRepository classModuleRepository;
    private final ClassModuleTeacherRepository classModuleTeacherRepository;
    private final NotificationRepository notificationRepository;
    private final SseService sseService;

    @Override
    @Transactional
    public JobLeadDTO create(Long studentId, JobLeadDTO dto) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sinh viên không tồn tại"));

        JobLead lead = new JobLead();
        lead.setStudent(student);
        lead.setCompanyName(dto.getCompanyName());
        lead.setShortName(dto.getShortName());
        lead.setAddress(dto.getAddress());
        lead.setWebsite(dto.getWebsite());

        lead.setStatus(JobLead.LeadStatus.NEW);
        lead.setCreatedAt(LocalDateTime.now());

        JobLead saved = jobLeadRepository.save(lead);

        String title = "Sinh viên tạo đầu mối việc làm mới";
        String content = "Sinh viên " + student.getUsername() + " đã tạo đầu mối việc làm tại công ty: "
                + saved.getCompanyName();
        notifyTeachers(student, title, content, Notification.NotificationType.INTERNSHIP_STATUS_UPDATE, saved.getId());

        return new JobLeadDTO(
                saved.getId(),
                saved.getCompanyName(),
                saved.getShortName(),
                saved.getAddress(),
                saved.getWebsite(),
                saved.getStatus().getDisplayName(),
                false,
                saved.getCreatedAt());
    }

    @Override
    public List<JobLeadDTO> getMyLeads(Long studentId) {
        List<JobLeadDTO> result = new ArrayList<>();

        List<JobLead> myLeads = jobLeadRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        for (JobLead lead : myLeads) {
            result.add(new JobLeadDTO(
                    lead.getId(),
                    lead.getCompanyName(),
                    lead.getShortName(),
                    lead.getAddress(),
                    lead.getWebsite(),
                    lead.getStatus().getDisplayName(),
                    false,
                    lead.getCreatedAt()));
        }

        List<JobApplication> adminReferrals = jobApplicationRepository.findByStudentId(studentId);
        for (JobApplication app : adminReferrals) {
            Company company = app.getCompany();
            result.add(new JobLeadDTO(
                    app.getId(),
                    company.getName(),
                    company.getShortName(),
                    company.getAddress(),
                    company.getWebsite(),
                    app.getStatus().getDisplayName(),
                    true,
                    app.getAppliedAt()));
        }

        return result.stream()
                .sorted(Comparator.comparing(JobLeadDTO::getDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long leadId, Long studentId) {
        JobLead lead = jobLeadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Đầu mối không tồn tại"));

        if (!lead.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("Bạn không có quyền xóa đầu mối này");
        }

        jobLeadRepository.delete(lead);
    }

    private void notifyTeachers(User student, String title, String content, Notification.NotificationType type,
            Long targetId) {
        List<ClassMember> classMembers = classMemberRepository.findByUserId(student.getId());
        Set<Long> notifiedTeacherIds = new HashSet<>();

        for (ClassMember member : classMembers) {
            Long classId = member.getClassId();
            List<ClassModule> modules = classModuleRepository.findByClassId(classId);

            for (ClassModule module : modules) {
                List<ClassModuleTeacher> teachers = classModuleTeacherRepository.findByClassModuleId(module.getId());
                for (ClassModuleTeacher teacher : teachers) {
                    if (!notifiedTeacherIds.contains(teacher.getUserId())) {
                        Notification notification = new Notification();
                        notification.setReceiverId(teacher.getUserId());
                        notification.setSenderId(student.getId());
                        notification.setNotificationType(type);
                        notification.setTargetType("JOB_LEAD");
                        notification.setTargetId(targetId);
                        notification.setTitle(title);
                        notification.setContent(content);
                        notification.setCreatedAt(LocalDateTime.now());
                        notification.setIsRead(false);

                        Notification savedNotification = notificationRepository.save(notification);
                        notifiedTeacherIds.add(teacher.getUserId());

                        // Gửi SSE
                        NotificationDTO dto = new NotificationDTO(
                                savedNotification.getId(),
                                savedNotification.getReceiverId(),
                                savedNotification.getSenderId(),
                                student.getUsername(),
                                student.getProfileImage(),
                                savedNotification.getNotificationType().name(),
                                savedNotification.getTargetType(),
                                savedNotification.getTargetId(),
                                savedNotification.getTitle(),
                                savedNotification.getContent(),
                                savedNotification.getIconUrl(),
                                savedNotification.getIsRead(),
                                savedNotification.getCreatedAt(),
                                savedNotification.getReadAt(),
                                null);
                        sseService.sendNotification(teacher.getUserId(), dto);
                    }
                }
            }
        }
    }
}