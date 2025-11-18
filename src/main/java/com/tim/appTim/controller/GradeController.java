package com.tim.appTim.controller;

import com.tim.appTim.dto.*;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.GradeService;
import com.tim.appTim.service.UserService;

import jakarta.validation.Valid; // (Giữ lại, nhưng chúng ta không dùng DTO có @Valid nữa)
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

    /**
     * API CHÍNH: Dùng cho "Trang nhập điểm" (cả Tạo và Cập nhật)
     * API: POST /grades/batch
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('grade:create')")
    public ResponseEntity<Void> batchCreateOrUpdateGrades(
            @RequestBody BatchGradeUpdateDTO batchDto, // DTO mới
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        gradeService.batchCreateOrUpdateGrades(batchDto, currentUser);
        return ResponseEntity.ok().build();
    }

    /**
     * API CHO GIÁO VIÊN: Lấy sổ điểm (có phân trang)
     * API: GET /grades/class-modules/{classModuleId}/gradebook
     */
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

    /**
     * API CHO SINH VIÊN: Tự xem điểm (1 hàng duy nhất)
     * API: GET /grades/class-modules/{classModuleId}/my-grades
     */
    @GetMapping("/class-modules/{classModuleId}/my-grades")
    @PreAuthorize("isAuthenticated()") // Chỉ cần đăng nhập
    public ResponseEntity<GradeDTO> getMyGradesInModule( // Sửa: Trả về 1 GradeDTO
                                                         @PathVariable Long classModuleId,
                                                         Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        // Sửa: Gọi hàm getMyGrades
        GradeDTO grade = gradeService.getMyGrades(classModuleId, currentUser.getId());
        return ResponseEntity.ok(grade);
    }

    /**
     * API XEM LỊCH SỬ: Dùng cho cả SV và GV
     * API: GET /grades/{gradeId}/history
     */
    @GetMapping("/{gradeId}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GradeHistoryDTO>> getGradeHistory(
            @PathVariable Long gradeId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        List<GradeHistoryDTO> history = gradeService.getGradeHistory(gradeId, currentUser);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{gradeId}")
    @PreAuthorize("hasAuthority('grade:delete')")
    public ResponseEntity<Void> deleteGrade(
            @PathVariable Long gradeId,
            Authentication authentication) {

        User currentUser = getUserFromAuthentication(authentication);
        gradeService.deleteGrade(gradeId, currentUser);

        // Trả về 204 No Content (thành công, không có nội dung trả về)
        return ResponseEntity.noContent().build();
    }

}