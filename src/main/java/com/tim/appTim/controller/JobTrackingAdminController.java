package com.tim.appTim.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.AdminJobLeadDTO;
import com.tim.appTim.dto.CreateJobLeadRequest;
import com.tim.appTim.dto.JobTrackingOverviewFilter;
import com.tim.appTim.dto.JobTrackingOverviewSummaryDTO;
import com.tim.appTim.dto.JobTrackingRowDTO;
import com.tim.appTim.dto.JobTrackingUpdateRequest;
import com.tim.appTim.service.JobTrackingAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/job-tracking")
@RequiredArgsConstructor
public class JobTrackingAdminController {

    private final JobTrackingAdminService jobTrackingAdminService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('job:read')")
    public ResponseEntity<JobTrackingOverviewSummaryDTO> getJobTrackingOverview(
            @RequestParam(value = "programId", required = false) Integer programId,
            @RequestParam(value = "mentorId", required = false) Long mentorId) {
        JobTrackingOverviewFilter filter = new JobTrackingOverviewFilter();
        filter.setProgramId(programId);
        filter.setMentorId(mentorId);
        return ResponseEntity.ok(jobTrackingAdminService.getJobTrackingOverview(filter));
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasAuthority('job:read')")
    public ResponseEntity<List<JobTrackingRowDTO>> getJobTrackingByClass(@PathVariable Long classId) {
        return ResponseEntity.ok(jobTrackingAdminService.getJobTrackingByClass(classId));
    }

    @PatchMapping("/classes/{classId}/students/{studentId}")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<JobTrackingRowDTO> updateJobInterest(
            @PathVariable Long classId,
            @PathVariable Long studentId,
            @RequestBody JobTrackingUpdateRequest request) {
        return ResponseEntity.ok(jobTrackingAdminService.updateJobInterest(classId, studentId, request));
    }

    @GetMapping("/classes/{classId}/students/{studentId}/leads")
    @PreAuthorize("hasAuthority('job:read')")
    public ResponseEntity<List<AdminJobLeadDTO>> getStudentLeads(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(jobTrackingAdminService.getStudentLeads(classId, studentId));
    }

    @PostMapping("/classes/{classId}/students/{studentId}/leads")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<AdminJobLeadDTO> createJobLead(
            @PathVariable Long classId,
            @PathVariable Long studentId,
            @RequestBody CreateJobLeadRequest request) {
        return ResponseEntity.ok(jobTrackingAdminService.createJobLead(
                classId,
                studentId,
                request.getCompanyName(),
                request.getShortName(),
                request.getAddress(),
                request.getWebsite()));
    }
}
