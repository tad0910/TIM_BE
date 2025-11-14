package com.tim.appTim.controller; // Gói controller của bạn

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
@RequestMapping("grades") // (Giữ nguyên mapping cũ)
public class GradeController {

    private final GradeService gradeService;
    private final UserService userService; // THÊM UserService

    // THÊM UserService vào constructor
    public GradeController(GradeService gradeService, UserService userService) {
        this.gradeService = gradeService;
        this.userService = userService;
    }

    // THÊM hàm helper giống hệt PostController
    private User getUserFromAuthentication(Authentication authentication) {
        // Dùng hàm của UserService mà bạn đã có
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    /**
     * Kịch bản 1: Sinh viên xem điểm của chính mình
     * API: GET /api/v1/class-modules/{classModuleId}/my-grades
     */
    @GetMapping("/class-modules/{classModuleId}/my-grades")
    @PreAuthorize("hasAuthority('grade:read_all')")
    public ResponseEntity<List<StudentGradeDTO>> getMyGradesInModule(
            @PathVariable Long classModuleId,
            Authentication authentication) {

        // --- BẮT ĐẦU PHẦN DEBUG ---
        // Lấy danh sách các quyền từ token
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        User currentUser = getUserFromAuthentication(authentication);
        Long studentId = currentUser.getId();

        List<StudentGradeDTO> grades = gradeService.getMyGrades(classModuleId, studentId);
        return ResponseEntity.ok(grades);
    }

    /**
     * Kịch bản 2: Giảng viên xem điểm của 1 sinh viên cụ thể
     * API: GET /api/v1/class-modules/{classModuleId}/grades?studentId=...
     */
    @GetMapping("/class-modules/{classModuleId}/grades")
    @PreAuthorize("hasAuthority('grade:read_detail')")
    public ResponseEntity<List<StudentGradeDTO>> getStudentGradesInModule(
            @PathVariable Long classModuleId,
            @RequestParam Long studentId,
            Authentication authentication) { // SỬA: Dùng Authentication

        // SỬA: Lấy user theo cách của bạn
        User currentUser = getUserFromAuthentication(authentication);
        Long teacherId = currentUser.getId();

        // Kiểm tra quyền của giảng viên
        gradeService.validateTeacherPermission(classModuleId, teacherId);

        List<StudentGradeDTO> grades = gradeService.getStudentGrades(classModuleId, studentId);
        return ResponseEntity.ok(grades);
    }

    /**
     * Kịch bản 3: Giảng viên xem sổ điểm (Gradebook) của cả lớp
     * API: GET /api/v1/class-modules/{classModuleId}/gradebook
     */
    @GetMapping("/class-modules/{classModuleId}/gradebook")
    @PreAuthorize("hasAuthority('grade:read_detail')")
    public ResponseEntity<GradebookDTO> getModuleGradebook(
            @PathVariable Long classModuleId,
            Authentication authentication) { // SỬA: Dùng Authentication

        // SỬA: Lấy user theo cách của bạn
        User currentUser = getUserFromAuthentication(authentication);
        Long teacherId = currentUser.getId();

        // Service sẽ tự kiểm tra quyền trong hàm getGradebook
        GradebookDTO gradebook = gradeService.getGradebook(classModuleId, teacherId);
        return ResponseEntity.ok(gradebook);
    }
}