package com.tim.appTim.service;

import java.util.List;

import com.tim.appTim.dto.JobTrackingRowDTO;
import com.tim.appTim.dto.JobTrackingUpdateRequest;

public interface JobTrackingAdminService {

    List<JobTrackingRowDTO> getJobTrackingByClass(Long classId);

    JobTrackingRowDTO updateJobInterest(Long classId, Long studentId, JobTrackingUpdateRequest request);
}
