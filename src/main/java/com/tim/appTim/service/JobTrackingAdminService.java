package com.tim.appTim.service;

import java.util.List;

import com.tim.appTim.dto.AdminJobLeadDTO;
import com.tim.appTim.dto.JobTrackingOverviewFilter;
import com.tim.appTim.dto.JobTrackingOverviewSummaryDTO;
import com.tim.appTim.dto.JobTrackingRowDTO;
import com.tim.appTim.dto.JobTrackingUpdateRequest;

public interface JobTrackingAdminService {

    List<JobTrackingRowDTO> getJobTrackingByClass(Long classId);

    JobTrackingRowDTO updateJobInterest(Long classId, Long studentId, JobTrackingUpdateRequest request);

    JobTrackingOverviewSummaryDTO getJobTrackingOverview(JobTrackingOverviewFilter filter);

    java.util.List<AdminJobLeadDTO> getStudentLeads(Long classId, Long studentId);

    AdminJobLeadDTO createJobLead(Long classId, Long studentId, String companyName, String shortName, String address, String website);
}
