package com.tim.appTim.service;

import com.tim.appTim.dto.JobActivityRequest;
import com.tim.appTim.entity.JobActivity;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface JobActivityService {
    JobActivity addActivity(JobActivityRequest request, MultipartFile file);
    List<JobActivity> getActivitiesByLead(Long jobLeadId);
    JobActivity updateNote(Long activityId, String note);
}