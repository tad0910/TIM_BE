package com.tim.appTim.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.JobTrackingRowDTO;
import com.tim.appTim.dto.JobTrackingUpdateRequest;
import com.tim.appTim.service.JobTrackingAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/job-tracking")
@RequiredArgsConstructor
public class JobTrackingAdminController {

    private final JobTrackingAdminService jobTrackingAdminService;

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
}
