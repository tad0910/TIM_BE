package com.tim.appTim.controller;

import com.tim.appTim.dto.ClassModuleScheduleDTO;
import com.tim.appTim.dto.ClassModuleScheduleTeacherDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.ClassModuleScheduleService;
import com.tim.appTim.service.ClassModuleScheduleTeacherService;
import com.tim.appTim.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;  
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedules")
public class ClassModuleScheduleController {

    private final ClassModuleScheduleService scheduleService;
    private final ClassModuleScheduleTeacherService scheduleTeacherService;
    private final UserService userService;

    public ClassModuleScheduleController(ClassModuleScheduleService scheduleService, 
                                        ClassModuleScheduleTeacherService scheduleTeacherService,
                                        UserService userService) {
        this.scheduleService = scheduleService;
        this.scheduleTeacherService = scheduleTeacherService;
        this.userService = userService;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('schedule:create')")
    public ResponseEntity<ClassModuleScheduleDTO> createSchedule(@RequestBody ClassModuleScheduleDTO scheduleDTO) {
        ClassModuleScheduleDTO createdSchedule = scheduleService.createSchedule(scheduleDTO);
        return new ResponseEntity<>(createdSchedule, HttpStatus.CREATED);
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAuthority('schedule:update')")
    public ResponseEntity<ClassModuleScheduleDTO> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ClassModuleScheduleDTO scheduleDTO) {

        ClassModuleScheduleDTO updatedSchedule = scheduleService.updateSchedule(scheduleId, scheduleDTO);
        return ResponseEntity.ok(updatedSchedule);
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassModuleScheduleDTO>> getSchedulesByClass(
            @PathVariable Long classId,
            @RequestParam(required = false) LocalDateTime startDate,  
            @RequestParam(required = false) LocalDateTime endDate) {  

        List<ClassModuleScheduleDTO> schedules = scheduleService.getSchedulesByClass(classId, startDate, endDate);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasAuthority('schedule:read_all') or @userService.isSelf(authentication, #instructorId)")
    public ResponseEntity<List<ClassModuleScheduleDTO>> getSchedulesByInstructor(
            @PathVariable Long instructorId,
            @RequestParam(required = false) LocalDateTime startDate,  
            @RequestParam(required = false) LocalDateTime endDate) {  

        List<ClassModuleScheduleDTO> schedules = scheduleService.getSchedulesByInstructor(instructorId, startDate, endDate);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/teacher/{teacherId}/all")
    @PreAuthorize("hasAuthority('schedule:read_all') or @userService.isSelf(authentication, #teacherId)")
    public ResponseEntity<List<ClassModuleScheduleDTO>> getAllSchedulesByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) LocalDateTime startDate,  
            @RequestParam(required = false) LocalDateTime endDate) {  

        List<ClassModuleScheduleDTO> schedules = scheduleService.getAllSchedulesByTeacher(teacherId, startDate, endDate);
        return ResponseEntity.ok(schedules);
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAuthority('schedule:delete')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{scheduleId}/teachers")
    @PreAuthorize("hasAuthority('schedule:update')")
    public ResponseEntity<ClassModuleScheduleTeacherDTO> assignTeacherToSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ClassModuleScheduleTeacherDTO dto) {
        ClassModuleScheduleTeacherDTO assigned = scheduleTeacherService.assignTeacherToSchedule(scheduleId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assigned);
    }

    @GetMapping("/{scheduleId}/teachers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassModuleScheduleTeacherDTO>> getScheduleTeachers(@PathVariable Long scheduleId) {
        List<ClassModuleScheduleTeacherDTO> teachers = scheduleTeacherService.getScheduleTeachers(scheduleId);
        return ResponseEntity.ok(teachers);
    }

    @DeleteMapping("/{scheduleId}/teachers/{userId}")
    @PreAuthorize("hasAuthority('schedule:update')")
    public ResponseEntity<Map<String, String>> removeTeacherFromSchedule(
            @PathVariable Long scheduleId,
            @PathVariable Long userId) {
        scheduleTeacherService.removeTeacherFromSchedule(scheduleId, userId);
        return ResponseEntity.ok(Map.of("message", "Xóa giáo viên khỏi buổi học thành công"));
    }

    @PutMapping("/{scheduleId}/teachers/{userId}/role")
    @PreAuthorize("hasAuthority('schedule:update')")
    public ResponseEntity<ClassModuleScheduleTeacherDTO> updateScheduleTeacherRole(
            @PathVariable Long scheduleId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        com.tim.appTim.entity.ClassModuleScheduleTeacher.ScheduleTeacherRole role = 
            com.tim.appTim.entity.ClassModuleScheduleTeacher.ScheduleTeacherRole.valueOf(request.get("role"));
        ClassModuleScheduleTeacherDTO updated = scheduleTeacherService.updateTeacherRole(scheduleId, userId, role);
        return ResponseEntity.ok(updated);
    }

}