package com.tim.appTim.controller;

import com.tim.appTim.dto.JobActivityDTO;
import com.tim.appTim.dto.JobActivityRequest;
import com.tim.appTim.service.JobActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/job-activities")
@RequiredArgsConstructor
public class JobActivityController {

    private final JobActivityService jobActivityService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@userService.ownsJobLead(authentication, #request.jobLeadId)")
    public ResponseEntity<JobActivityDTO> addActivity(
            @RequestPart("data") JobActivityRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        return ResponseEntity.ok(jobActivityService.addActivity(request, file));
    }

    @GetMapping("/{jobLeadId}")
    @PreAuthorize("@userService.ownsJobLead(authentication, #jobLeadId)")
    public ResponseEntity<List<JobActivityDTO>> getActivities(@PathVariable Long jobLeadId) {
        return ResponseEntity.ok(jobActivityService.getActivitiesByLead(jobLeadId));
    }

    @PutMapping("/{id}/note")
    @PreAuthorize("@userService.ownsJobActivity(authentication, #id)")
    public ResponseEntity<JobActivityDTO> updateNote(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String note = request.get("note");
        return ResponseEntity.ok(jobActivityService.updateNote(id, note));
    }
}