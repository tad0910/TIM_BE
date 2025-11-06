package com.tim.appTim.controller;

import com.tim.appTim.dto.ClassModuleDTO;
import com.tim.appTim.dto.ClassModuleTeacherDTO;
import com.tim.appTim.entity.ClassModuleTeacher;
import com.tim.appTim.service.ClassModuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/classes/{classId}/modules")
public class ClassModuleController {

    private final ClassModuleService classModuleService;

    public ClassModuleController(ClassModuleService classModuleService) {
        this.classModuleService = classModuleService;
    }

    /**
     * Tạo ClassModule từ program của lớp
     */
    @PostMapping("/from-program")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<List<ClassModuleDTO>> createModulesFromProgram(@PathVariable Long classId) {
        List<ClassModuleDTO> created = classModuleService.createClassModulesFromProgram(classId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Tạo ClassModule thủ công
     */
    @PostMapping
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<ClassModuleDTO> createClassModule(
            @PathVariable Long classId,
            @RequestBody ClassModuleDTO dto) {
        dto.setClassId(classId);
        ClassModuleDTO created = classModuleService.createClassModule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Lấy danh sách ClassModule của lớp
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassModuleDTO>> getClassModules(@PathVariable Long classId) {
        List<ClassModuleDTO> modules = classModuleService.getClassModulesByClassId(classId);
        return ResponseEntity.ok(modules);
    }

    /**
     * Lấy ClassModule theo ID
     */
    @GetMapping("/{classModuleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClassModuleDTO> getClassModule(
            @PathVariable Long classId,
            @PathVariable Long classModuleId) {
        ClassModuleDTO module = classModuleService.getClassModuleById(classModuleId);
        return ResponseEntity.ok(module);
    }

    /**
     * Cập nhật ClassModule
     */
    @PutMapping("/{classModuleId}")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<ClassModuleDTO> updateClassModule(
            @PathVariable Long classId,
            @PathVariable Long classModuleId,
            @RequestBody ClassModuleDTO dto) {
        ClassModuleDTO updated = classModuleService.updateClassModule(classModuleId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Xóa ClassModule
     */
    @DeleteMapping("/{classModuleId}")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<Map<String, String>> deleteClassModule(
            @PathVariable Long classId,
            @PathVariable Long classModuleId) {
        classModuleService.deleteClassModule(classModuleId);
        return ResponseEntity.ok(Map.of("message", "Xóa ClassModule thành công"));
    }

    /**
     * Gán giáo viên vào ClassModule
     */
    @PostMapping("/{classModuleId}/teachers")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<ClassModuleTeacherDTO> assignTeacher(
            @PathVariable Long classId,
            @PathVariable Long classModuleId,
            @RequestBody ClassModuleTeacherDTO dto) {
        ClassModuleTeacherDTO assigned = classModuleService.assignTeacherToClassModule(classModuleId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assigned);
    }

    /**
     * Lấy danh sách giáo viên của ClassModule
     */
    @GetMapping("/{classModuleId}/teachers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassModuleTeacherDTO>> getClassModuleTeachers(
            @PathVariable Long classId,
            @PathVariable Long classModuleId) {
        List<ClassModuleTeacherDTO> teachers = classModuleService.getClassModuleTeachers(classModuleId);
        return ResponseEntity.ok(teachers);
    }

    /**
     * Xóa giáo viên khỏi ClassModule
     */
    @DeleteMapping("/{classModuleId}/teachers/{userId}")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<Map<String, String>> removeTeacher(
            @PathVariable Long classId,
            @PathVariable Long classModuleId,
            @PathVariable Long userId) {
        classModuleService.removeTeacherFromClassModule(classModuleId, userId);
        return ResponseEntity.ok(Map.of("message", "Xóa giáo viên khỏi ClassModule thành công"));
    }

    /**
     * Cập nhật vai trò giáo viên
     */
    @PutMapping("/{classModuleId}/teachers/{userId}/role")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<ClassModuleTeacherDTO> updateTeacherRole(
            @PathVariable Long classId,
            @PathVariable Long classModuleId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        ClassModuleTeacher.TeacherRole role = ClassModuleTeacher.TeacherRole.valueOf(request.get("role"));
        ClassModuleTeacherDTO updated = classModuleService.updateTeacherRole(classModuleId, userId, role);
        return ResponseEntity.ok(updated);
    }
}

