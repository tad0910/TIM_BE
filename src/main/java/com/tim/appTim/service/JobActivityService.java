package com.tim.appTim.service;

import com.tim.appTim.dto.JobActivityDTO;
import com.tim.appTim.dto.JobActivityRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface JobActivityService {
    JobActivityDTO addActivity(JobActivityRequest request, MultipartFile file);
    List<JobActivityDTO> getActivitiesByLead(Long jobLeadId);
    JobActivityDTO updateNote(Long activityId, String note);
}