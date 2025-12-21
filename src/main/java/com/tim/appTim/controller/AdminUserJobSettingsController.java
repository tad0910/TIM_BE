package com.tim.appTim.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.UpdateUserJobSettingsRequest;
import com.tim.appTim.dto.UserJobSettingsDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserJobSettingsController {

    private final UserRepository userRepository;

    @PatchMapping("/{studentId}/job-settings")
    @PreAuthorize("hasAuthority('job:update')")
    public ResponseEntity<UserJobSettingsDTO> updateUserJobSettings(
            @PathVariable Long studentId,
            @RequestBody UpdateUserJobSettingsRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));

        student.setJobInterestEnabled(request.isJobInterestEnabled());
        userRepository.save(student);

        return ResponseEntity.ok(new UserJobSettingsDTO(student.isJobInterestEnabled()));
    }
}
