package com.tim.appTim.controller;

import com.tim.appTim.dto.ClassModuleScheduleDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.ClassModuleScheduleService;
import com.tim.appTim.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ClassModuleScheduleController {

    private final ClassModuleScheduleService scheduleService;
    private final UserService userService;

    public ClassModuleScheduleController(ClassModuleScheduleService scheduleService, UserService userService) {
        this.scheduleService = scheduleService;
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
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        List<ClassModuleScheduleDTO> schedules = scheduleService.getSchedulesByClass(classId, startDate, endDate);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasAuthority('schedule:read_all') or @userService.isSelf(authentication, #instructorId)")
    public ResponseEntity<List<ClassModuleScheduleDTO>> getSchedulesByInstructor(
            @PathVariable Long instructorId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        List<ClassModuleScheduleDTO> schedules = scheduleService.getSchedulesByInstructor(instructorId, startDate, endDate);
        return ResponseEntity.ok(schedules);
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAuthority('schedule:delete')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok().build();
    }


}