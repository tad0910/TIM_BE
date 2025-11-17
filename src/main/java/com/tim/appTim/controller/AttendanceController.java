package com.tim.appTim.controller;

import com.tim.appTim.dto.AttendanceHistoryDto;
import com.tim.appTim.dto.AttendanceStatsDto;
import com.tim.appTim.dto.MarkAttendanceRequest;
import com.tim.appTim.entity.AttendanceRecord;
import com.tim.appTim.entity.AttendanceSession;
import com.tim.appTim.service.AttendanceService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/schedules/{scheduleId}/open")
    @PreAuthorize("hasAuthority('attendance:open') or " +
            "(authentication.authenticated and @attendanceService.isScheduleTeacher(authentication, #scheduleId))")  
    public ResponseEntity<AttendanceSession> openAttendanceSession(
            @PathVariable Long scheduleId,
            @RequestBody @Valid Map<String, Integer> request, 
            Authentication authentication) { 

        Integer teacherId = request.get("teacherId");

        AttendanceSession session = attendanceService.openAttendanceSession(scheduleId, teacherId, authentication);
        
        return ResponseEntity.ok(session);
    }

    @PostMapping("/schedules/{scheduleId}/mark")
    @PreAuthorize("hasAuthority('attendance:mark') or " +
              "(authentication.authenticated and @attendanceService.isScheduleTeacher(authentication, #scheduleId))") 
    public ResponseEntity<List<AttendanceRecord>> markAttendance(
            @PathVariable Long scheduleId,
            @RequestBody @Valid MarkAttendanceRequest request,
            Authentication authentication) { 

        List<AttendanceRecord> records = attendanceService.markAttendanceBatch(scheduleId, request, authentication);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/history/{classId}")
    @PreAuthorize("hasAuthority('attendance:read_all')")
    public ResponseEntity<List<AttendanceHistoryDto>> getAttendanceHistory(@PathVariable Integer classId) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(classId));
    }

    @GetMapping("/stats/{classId}")
    @PreAuthorize("hasAuthority('attendance:read_all')")
    public ResponseEntity<List<AttendanceStatsDto>> getAttendanceStats(@PathVariable Integer classId) {
        return ResponseEntity.ok(attendanceService.getAttendanceStats(classId));
    }
    
}