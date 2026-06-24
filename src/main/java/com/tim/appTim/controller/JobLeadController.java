package com.tim.appTim.controller;

import com.tim.appTim.dto.request.JobLeadDTO;
import com.tim.appTim.service.JobLeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/job-leads")
@RequiredArgsConstructor
public class JobLeadController {

    private final JobLeadService jobLeadService;

    @GetMapping("/my-leads/{studentId}")
    @PreAuthorize("@userService.isSelf(authentication, #studentId)")
    public ResponseEntity<List<JobLeadDTO>> getMyLeads(@PathVariable Long studentId) {
        return ResponseEntity.ok(jobLeadService.getMyLeads(studentId));
    }

    @PostMapping("/{studentId}")
    @PreAuthorize("@userService.isSelf(authentication, #studentId)")
    public ResponseEntity<JobLeadDTO> createLead(@PathVariable Long studentId, @RequestBody JobLeadDTO request) {
        return ResponseEntity.ok(jobLeadService.create(studentId, request));
    }

    @DeleteMapping("/{leadId}/student/{studentId}")
    @PreAuthorize("@userService.isSelf(authentication, #studentId)")
    public ResponseEntity<?> deleteLead(@PathVariable Long leadId, @PathVariable Long studentId) {
        jobLeadService.delete(leadId, studentId);
        return ResponseEntity.ok("Đã xóa thành công");
    }
}
