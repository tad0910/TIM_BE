package com.tim.appTim.controller;

import com.tim.appTim.dto.FeeAdjustmentDTO;
import com.tim.appTim.entity.StudentTuition;
import com.tim.appTim.service.StudentTuitionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/student-tuition")
public class StudentTuitionController {

    @Autowired
    private StudentTuitionService studentTuitionService;

    @PostMapping("/register")
    public ResponseEntity<?> registerStudent(@RequestBody Map<String, Object> payload) {

        Long studentId = Long.valueOf(payload.get("studentId").toString());
        Long routeId = Long.valueOf(payload.get("routeId").toString());

        LocalDate enrollmentDate = LocalDate.now();
        if (payload.containsKey("enrollmentDate")) {
            enrollmentDate = LocalDate.parse(payload.get("enrollmentDate").toString());
        }

        String couponCode = null;
        if (payload.containsKey("couponCode")) {
            couponCode = payload.get("couponCode").toString();
        }

        StudentTuition result = studentTuitionService.registerStudent(studentId, routeId, enrollmentDate, couponCode);

        return ResponseEntity.ok(Map.of(
                "message", "Đăng ký thành công!",
                "profileId", result.getId(),
                "totalFee", result.getTotalActualFee(),
                "couponApplied", (couponCode != null ? couponCode : "Không")
        ));
    }

    @PutMapping("/adjust-fee")
    @PreAuthorize("hasAnyAuthority('tuition:update', 'ROLE_ADMIN')")
    public ResponseEntity<?> adjustTuitionFee(@RequestBody @Valid FeeAdjustmentDTO dto) {

        studentTuitionService.adjustRemainingFee(dto);

        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật học phí thành công!",
                "studentTuitionId", dto.getStudentTuitionId()
        ));
    }
}