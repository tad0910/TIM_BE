package com.tim.appTim.controller;

import com.tim.appTim.dto.common.StudentJobTrackingDTO;
import com.tim.appTim.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/job-applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @GetMapping("/tracking/{classId}")
    @PreAuthorize("hasAuthority('job:read')")
    public ResponseEntity<List<StudentJobTrackingDTO>> getJobTrackingList(@PathVariable Long classId) {
        return ResponseEntity.ok(jobApplicationService.getJobTrackingList(classId));
    }
    @PostMapping(value = "/introduce", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('job:create')")
    public ResponseEntity<?> introduceStudentsToCompany(
            @RequestParam("classId") Long classId,
            @RequestParam("companyId") Long companyId,
            @RequestParam("studentIds") List<Long> studentIds,
            @RequestParam(value = "cvFiles", required = false) List<MultipartFile> cvFiles
    ) {
        if (cvFiles != null && studentIds.size() != cvFiles.size()) {
            return ResponseEntity.badRequest().body("Số lượng học viên và số lượng CV không khớp!");
        }

        jobApplicationService.introduceBatch(classId, companyId, studentIds, cvFiles);

        return ResponseEntity.ok("Giới thiệu thành công!");
    }

}
