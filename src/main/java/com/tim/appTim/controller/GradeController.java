package com.tim.appTim.controller; // Gói controller của bạn

import com.tim.appTim.dto.GradeHistoryDTO;
import com.tim.appTim.dto.GradeUpdateDTO;
import com.tim.appTim.dto.GradebookDTO;
import com.tim.appTim.dto.StudentGradeDTO;
import com.tim.appTim.entity.User; // Import User entity
import com.tim.appTim.service.GradeService;
import com.tim.appTim.service.UserService; // Import UserService

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/class-modules/{classModuleId}/my-grades")
    @PreAuthorize("hasAuthority('grade:read_all')")
    public ResponseEntity<List<StudentGradeDTO>> getMyGradesInModule(
            @PathVariable Long classModuleId,
            Authentication authentication) {

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        User currentUser = getUserFromAuthentication(authentication);
        Long studentId = currentUser.getId();

        List<StudentGradeDTO> grades = gradeService.getMyGrades(classModuleId, studentId);
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/class-modules/{classModuleId}/grades")
    @PreAuthorize("hasAuthority('grade:read_detail')")
    public ResponseEntity<List<StudentGradeDTO>> getStudentGradesInModule(
            @PathVariable Long classModuleId,
            @RequestParam Long studentId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        Long teacherId = currentUser.getId();

        gradeService.validateTeacherPermission(classModuleId, teacherId);

        List<StudentGradeDTO> grades = gradeService.getStudentGrades(classModuleId, studentId);
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/class-modules/{classModuleId}/gradebook")
    @PreAuthorize("hasAuthority('grade:read_detail')")
    public ResponseEntity<GradebookDTO> getModuleGradebook(
            @PathVariable Long classModuleId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        Long teacherId = currentUser.getId();

        GradebookDTO gradebook = gradeService.getGradebook(classModuleId, teacherId);
        return ResponseEntity.ok(gradebook);
    }

    @PutMapping("/{gradeId}")
    @PreAuthorize("hasAuthority('grade:update')")
    public ResponseEntity<StudentGradeDTO> updateGrade(
            @PathVariable Long gradeId,
            @RequestBody GradeUpdateDTO gradeUpdateDTO,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);

        StudentGradeDTO updatedGrade = gradeService.updateGrade(gradeId, gradeUpdateDTO, currentUser);

        return ResponseEntity.ok(updatedGrade);
    }

    @GetMapping("/{gradeId}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GradeHistoryDTO>> getGradeHistory(
            @PathVariable Long gradeId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);

        List<GradeHistoryDTO> history = gradeService.getGradeHistory(gradeId, currentUser);

        return ResponseEntity.ok(history);
    }
}