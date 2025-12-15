package com.tim.appTim.controller;

import com.tim.appTim.dto.*;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.GradeService;
import com.tim.appTim.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("grades")
public class GradeController {
    private final GradeService gradeService;
    private final UserService userService;

    public GradeController(GradeService gradeService, UserService userService) {
        this.gradeService = gradeService;
        this.userService = userService;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('grade:create')")
    public ResponseEntity<Void> batchCreateOrUpdateGrades(
            @RequestBody BatchGradeUpdateDTO batchDto,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        gradeService.batchCreateOrUpdateGrades(batchDto, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/class-modules/{classModuleId}/gradebook")
    @PreAuthorize("hasAuthority('grade:read_detail')")
    public ResponseEntity<GradebookDTO> getModuleGradebook(
            @PathVariable Long classModuleId,
            Authentication authentication,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        User currentUser = getUserFromAuthentication(authentication);
        GradebookDTO gradebook = gradeService.getGradebook(classModuleId, currentUser.getId(), pageable);
        return ResponseEntity.ok(gradebook);
    }

    @GetMapping("/class-modules/{classModuleId}/my-grades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GradeDTO> getMyGradesInModule(
            @PathVariable Long classModuleId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        GradeDTO grade = gradeService.getMyGrades(classModuleId, currentUser.getId());
        return ResponseEntity.ok(grade);
    }

    @GetMapping("/{gradeId}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<GradeHistoryDTO>> getGradeHistory(
            @PathVariable Long gradeId,
            Authentication authentication,
            @PageableDefault(size = 20, sort = "changedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        User currentUser = getUserFromAuthentication(authentication);
        Page<GradeHistoryDTO> history = gradeService.getGradeHistory(gradeId, currentUser, pageable);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{gradeId}")
    @PreAuthorize("hasAuthority('grade:delete')")
    public ResponseEntity<Void> deleteGrade(
            @PathVariable Long gradeId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        gradeService.deleteGrade(gradeId, currentUser);

        return ResponseEntity.noContent().build();
    }

}