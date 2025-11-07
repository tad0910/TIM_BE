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

    @PostMapping("/from-program")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<List<ClassModuleDTO>> createModulesFromProgram(@PathVariable Long classId) {
        List<ClassModuleDTO> created = classModuleService.createClassModulesFromProgram(classId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<ClassModuleDTO> createClassModule(
            @PathVariable Long classId,
            @RequestBody ClassModuleDTO dto) {
        dto.setClassId(classId);
        ClassModuleDTO created = classModuleService.createClassModule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassModuleDTO>> getClassModules(@PathVariable Long classId) {
        List<ClassModuleDTO> modules = classModuleService.getClassModulesByClassId(classId);
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/{classModuleId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClassModuleDTO> getClassModule(
            @PathVariable Long classId,
            @PathVariable Long classModuleId) {
        ClassModuleDTO module = classModuleService.getClassModuleById(classModuleId);
        return ResponseEntity.ok(module);
    }

    @PutMapping("/{classModuleId}")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<ClassModuleDTO> updateClassModule(
            @PathVariable Long classId,
            @PathVariable Long classModuleId,
            @RequestBody ClassModuleDTO dto) {
        ClassModuleDTO updated = classModuleService.updateClassModule(classModuleId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{classModuleId}")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<Map<String, String>> deleteClassModule(
            @PathVariable Long classId,
            @PathVariable Long classModuleId) {
        classModuleService.deleteClassModule(classModuleId);
        return ResponseEntity.ok(Map.of("message", "Xóa ClassModule thành công"));
    }

    @PostMapping("/{classModuleId}/teachers")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<ClassModuleTeacherDTO> assignTeacher(
            @PathVariable Long classId,
            @PathVariable Long classModuleId,
            @RequestBody ClassModuleTeacherDTO dto) {
        ClassModuleTeacherDTO assigned = classModuleService.assignTeacherToClassModule(classModuleId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assigned);
    }

    @GetMapping("/{classModuleId}/teachers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassModuleTeacherDTO>> getClassModuleTeachers(
            @PathVariable Long classId,
            @PathVariable Long classModuleId) {
        List<ClassModuleTeacherDTO> teachers = classModuleService.getClassModuleTeachers(classModuleId);
        return ResponseEntity.ok(teachers);
    }

    @DeleteMapping("/{classModuleId}/teachers/{userId}")
    @PreAuthorize("hasAuthority('class:update_all')")
    public ResponseEntity<Map<String, String>> removeTeacher(
            @PathVariable Long classId,
            @PathVariable Long classModuleId,
            @PathVariable Long userId) {
        classModuleService.removeTeacherFromClassModule(classModuleId, userId);
        return ResponseEntity.ok(Map.of("message", "Xóa giáo viên khỏi ClassModule thành công"));
    }

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

