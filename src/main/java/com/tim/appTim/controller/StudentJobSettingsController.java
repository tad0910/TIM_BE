package com.tim.appTim.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tim.appTim.dto.request.UpdateUserJobSettingsRequest;
import com.tim.appTim.dto.common.UserJobSettingsDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.UnauthorizedException;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/job-settings")
@RequiredArgsConstructor
public class StudentJobSettingsController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserJobSettingsDTO> getMyJobSettings(Authentication authentication) {
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            throw new UnauthorizedException("Không tìm thấy thông tin người dùng");
        }
        return ResponseEntity.ok(new UserJobSettingsDTO(currentUser.isJobInterestEnabled()));
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserJobSettingsDTO> updateMyJobSettings(
            Authentication authentication,
            @RequestBody UpdateUserJobSettingsRequest request) {
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            throw new UnauthorizedException("Không tìm thấy thông tin người dùng");
        }

        currentUser.setJobInterestEnabled(request.isJobInterestEnabled());
        userRepository.save(currentUser);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new UserJobSettingsDTO(currentUser.isJobInterestEnabled()));
    }
}

