package com.tim.appTim.service;

import com.tim.appTim.dto.JobActivityDTO;
import com.tim.appTim.dto.JobActivityRequest;
import com.tim.appTim.dto.NotificationDTO;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.ClassModule;
import com.tim.appTim.entity.ClassModuleTeacher;
import com.tim.appTim.entity.JobActivity;
import com.tim.appTim.entity.JobActivityType;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.User;

import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassMemberRepository;
import com.tim.appTim.repository.ClassModuleRepository;
import com.tim.appTim.repository.ClassModuleTeacherRepository;
import com.tim.appTim.repository.JobActivityRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.repository.NotificationRepository;
import com.tim.appTim.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobActivityServiceImpl implements JobActivityService {

    private final JobActivityRepository jobActivityRepository;
    private final JobLeadRepository jobLeadRepository;
    private final FileUploadService fileUploadService;
    private final ClassMemberRepository classMemberRepository;
    private final ClassModuleRepository classModuleRepository;
    private final ClassModuleTeacherRepository classModuleTeacherRepository;
    private final NotificationRepository notificationRepository;
    private final SseService sseService;
    private final UserService userService;

    @Override
    @Transactional
    public JobActivityDTO addActivity(JobActivityRequest request, MultipartFile file) {
        JobLead jobLead = jobLeadRepository.findById(request.getJobLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("Đầu mối không tồn tại"));

        ensureLeadOwnership(jobLead);

        JobActivity activity = new JobActivity();
        activity.setJobLead(jobLead);
        activity.setContent(request.getContent());
        activity.setHappenedAt(request.getHappenedAt());
        activity.setSalaryAmount(request.getSalaryAmount());
        activity.setCreatedAt(LocalDateTime.now());

        try {
            JobActivityType activityType = JobActivityType.valueOf(request.getActivityType());
            activity.setActivityType(activityType);

            mapActivityToLeadStatus(activityType, jobLead);
            jobLeadRepository.save(jobLead);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái hoạt động không hợp lệ: " + request.getActivityType());
        }

        if (file != null && !file.isEmpty()) {
            try {
                String fileUrl = fileUploadService.uploadFile(file);
                activity.setFileUrl(fileUrl);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload file đính kèm");
            }
        }

        JobActivity savedActivity = jobActivityRepository.save(activity);

        String title = "Cập nhật thực tập: " + jobLead.getStudent().getUsername();
        String content = "Sinh viên " + jobLead.getStudent().getUsername() + " vừa cập nhật trạng thái: "
                + activity.getActivityType().getDisplayName();
        notifyTeachers(jobLead.getStudent(), title, content, Notification.NotificationType.INTERNSHIP_STATUS_UPDATE,
                savedActivity.getId());

        return toDto(savedActivity);
    }

    @Override
    public List<JobActivityDTO> getActivitiesByLead(Long jobLeadId) {
        ensureLeadOwnership(jobLeadId);

        return jobActivityRepository.findByJobLeadIdOrderByCreatedAtDesc(jobLeadId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobActivityDTO updateNote(Long activityId, String note) {
        JobActivity activity = jobActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Hoạt động không tồn tại"));

        ensureLeadOwnership(activity.getJobLead());

        activity.setNote(note);
        JobActivity savedActivity = jobActivityRepository.save(activity);

        String title = "Cập nhật nhật ký thực tập";
        String content = "Sinh viên " + activity.getJobLead().getStudent().getUsername()
                + " đã cập nhật ghi chú cho hoạt động thực tập.";
        notifyTeachers(activity.getJobLead().getStudent(), title, content,
                Notification.NotificationType.INTERNSHIP_LOG_UPDATE, savedActivity.getId());

        return toDto(savedActivity);
    }

    private JobActivityDTO toDto(JobActivity activity) {
        return new JobActivityDTO(
                activity.getId(),
                activity.getJobLeadId(),
                activity.getActivityType() != null ? activity.getActivityType().name() : null,
                activity.getContent(),
                activity.getHappenedAt(),
                activity.getCreatedAt(),
                activity.getSalaryAmount(),
                activity.getNote(),
                activity.getFileUrl()
        );
    }

    private void mapActivityToLeadStatus(JobActivityType activityType, JobLead jobLead) {
        JobLead.LeadStatus targetStatus = switch (activityType) {
            case SEND_CV -> JobLead.LeadStatus.APPLIED;
            case INTERVIEW_SCHEDULED, INTERVIEW -> JobLead.LeadStatus.INTERVIEWING;
            case OFFER_RECEIVED -> JobLead.LeadStatus.OFFER;
            case PROBATION_CONTRACT -> JobLead.LeadStatus.PROBATION;
            case OFFICIAL_CONTRACT -> JobLead.LeadStatus.OFFICIAL;
        };

        if (shouldUpdateStatus(jobLead.getStatus(), targetStatus)) {
            jobLead.setStatus(targetStatus);
        }
    }

    private boolean shouldUpdateStatus(JobLead.LeadStatus currentStatus, JobLead.LeadStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        if (currentStatus == null) {
            return true;
        }

        // Preserve terminal states FAILED/IGNORED when logging additional notes
        if (currentStatus == JobLead.LeadStatus.FAILED || currentStatus == JobLead.LeadStatus.IGNORED) {
            return false;
        }

        return currentStatus.ordinal() <= targetStatus.ordinal();
    }

    private void ensureLeadOwnership(JobLead jobLead) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean ownsLead = userService.ownsJobLead(authentication, jobLead.getId());
        if (!ownsLead) {
            throw new AccessDeniedException("Bạn không có quyền truy cập đầu mối này");
        }
    }

    private void ensureLeadOwnership(Long jobLeadId) {
        JobLead jobLead = jobLeadRepository.findById(jobLeadId)
                .orElseThrow(() -> new ResourceNotFoundException("Đầu mối không tồn tại"));
        ensureLeadOwnership(jobLead);
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
                        notification.setTargetType("JOB_ACTIVITY");
                        notification.setTargetId(targetId);
                        notification.setTitle(title);
                        notification.setContent(content);
                        notification.setCreatedAt(LocalDateTime.now());
                        notification.setIsRead(false);

                        Notification savedNotification = notificationRepository.save(notification);
                        notifiedTeacherIds.add(teacher.getUserId());

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