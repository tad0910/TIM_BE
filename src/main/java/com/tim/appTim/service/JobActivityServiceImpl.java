package com.tim.appTim.service;

import com.tim.appTim.dto.JobActivityRequest;
import com.tim.appTim.dto.NotificationDTO;
import com.tim.appTim.entity.JobActivity;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.repository.JobActivityRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import com.tim.appTim.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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

    @Override
    @Transactional
    public JobActivity addActivity(JobActivityRequest request, MultipartFile file) {
        JobLead jobLead = jobLeadRepository.findById(request.getJobLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("Đầu mối không tồn tại"));

        JobActivity activity = new JobActivity();
        activity.setJobLead(jobLead);
        activity.setContent(request.getContent());
        activity.setHappenedAt(request.getHappenedAt());
        activity.setSalaryAmount(request.getSalaryAmount());
        activity.setCreatedAt(LocalDateTime.now());

        try {
            JobLead.LeadStatus status = JobLead.LeadStatus.valueOf(request.getActivityType());
            activity.setActivityType(status);
            jobLead.setStatus(status);
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

        return savedActivity;
    }

    @Override
    public List<JobActivity> getActivitiesByLead(Long jobLeadId) {
        return jobActivityRepository.findByJobLeadIdOrderByHappenedAtDesc(jobLeadId);
    }

    @Override
    public JobActivity updateNote(Long activityId, String note) {
        JobActivity activity = jobActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Hoạt động không tồn tại"));

        activity.setNote(note);
        JobActivity savedActivity = jobActivityRepository.save(activity);

        String title = "Cập nhật nhật ký thực tập";
        String content = "Sinh viên " + activity.getJobLead().getStudent().getUsername()
                + " đã cập nhật ghi chú cho hoạt động thực tập.";
        notifyTeachers(activity.getJobLead().getStudent(), title, content,
                Notification.NotificationType.INTERNSHIP_LOG_UPDATE, savedActivity.getId());

        return savedActivity;
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