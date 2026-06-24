package com.tim.appTim.service;

import com.tim.appTim.dto.common.JobActivityDTO;
import com.tim.appTim.dto.request.JobActivityRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface JobActivityService {
    JobActivityDTO addActivity(JobActivityRequest request, MultipartFile file);
    List<JobActivityDTO> getActivitiesByLead(Long jobLeadId);
    JobActivityDTO updateNote(Long activityId, String note);
}
