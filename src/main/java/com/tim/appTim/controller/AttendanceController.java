package com.tim.appTim.controller;

import com.tim.appTim.dto.AttendanceHistoryDto;
import com.tim.appTim.dto.AttendanceStatsDto;
import com.tim.appTim.dto.MarkAttendanceRequest;
import com.tim.appTim.entity.AttendanceRecord;
import com.tim.appTim.entity.AttendanceSession;
import com.tim.appTim.service.AttendanceService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;
    
    @PostMapping("/schedules/{scheduleId}/open")
    public ResponseEntity<AttendanceSession> openAttendanceSession(
            @PathVariable Long scheduleId,
            @RequestBody @Valid Map<String, Integer> request) {

        Integer teacherId = request.get("teacherId");
        AttendanceSession session = attendanceService.openAttendanceSession(scheduleId, teacherId);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/schedules/{scheduleId}/mark")
    public ResponseEntity<List<AttendanceRecord>> markAttendance(
            @PathVariable Long scheduleId,
            @RequestBody @Valid MarkAttendanceRequest request) {

        List<AttendanceRecord> records = attendanceService.markAttendanceBatch(scheduleId, request);
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/history/{classId}")
    public ResponseEntity<List<AttendanceHistoryDto>> getAttendanceHistory(@PathVariable Integer classId) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistory(classId));
    }

    @GetMapping("/stats/{classId}")
    public ResponseEntity<List<AttendanceStatsDto>> getAttendanceStats(@PathVariable Integer classId) {
        return ResponseEntity.ok(attendanceService.getAttendanceStats(classId));
    }

}