package com.tim.appTim.service;

import com.tim.appTim.dto.JobActivityRequest;
import com.tim.appTim.entity.JobActivity;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.repository.JobActivityRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.service.FileUploadService;
import com.tim.appTim.service.JobActivityService;
import com.tim.appTim.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobActivityServiceImpl implements JobActivityService {

    private final JobActivityRepository jobActivityRepository;
    private final JobLeadRepository jobLeadRepository;
    private final FileUploadService fileUploadService;

    @Override
    @Transactional
    public JobActivity addActivity(JobActivityRequest request, MultipartFile file) {
        // 1. Tìm đầu mối cha
        JobLead jobLead = jobLeadRepository.findById(request.getJobLeadId())
                .orElseThrow(() -> new ResourceNotFoundException("Đầu mối không tồn tại"));

        // 2. Tạo Activity mới
        JobActivity activity = new JobActivity();
        activity.setJobLead(jobLead);
        activity.setContent(request.getContent());
        activity.setHappenedAt(request.getHappenedAt());
        activity.setSalaryAmount(request.getSalaryAmount()); // Lưu mức lương/offer
        activity.setCreatedAt(LocalDateTime.now());

        // Convert String status sang Enum
        try {
            JobLead.LeadStatus status = JobLead.LeadStatus.valueOf(request.getActivityType());
            activity.setActivityType(status);

            // [QUAN TRỌNG] Cập nhật trạng thái mới nhất cho JobLead cha
            // Ví dụ: Đang là "Gửi CV", thêm hoạt động "Nhận Offer" -> JobLead chuyển sang "Nhận Offer"
            jobLead.setStatus(status);
            jobLeadRepository.save(jobLead);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Trạng thái hoạt động không hợp lệ: " + request.getActivityType());
        }

        // 3. Upload file đính kèm (nếu có)
        if (file != null && !file.isEmpty()) {
            try {
                String fileUrl = fileUploadService.uploadFile(file);
                activity.setFileUrl(fileUrl);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload file đính kèm");
            }
        }

        return jobActivityRepository.save(activity);
    }

    @Override
    public List<JobActivity> getActivitiesByLead(Long jobLeadId) {
        // Sắp xếp giảm dần theo ngày diễn ra (Mới nhất lên đầu)
        return jobActivityRepository.findByJobLeadIdOrderByHappenedAtDesc(jobLeadId);
    }

    @Override
    public JobActivity updateNote(Long activityId, String note) {
        JobActivity activity = jobActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Hoạt động không tồn tại"));

        activity.setNote(note);
        return jobActivityRepository.save(activity);
    }

}