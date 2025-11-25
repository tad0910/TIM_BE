package com.tim.appTim.controller;

import com.tim.appTim.dto.TuitionOverviewDTO;
import com.tim.appTim.service.TuitionTransactionService;
import com.tim.appTim.service.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tuition-overview")
public class TuitionOverviewController {

    private final TuitionTransactionService transactionService;

    public TuitionOverviewController(TuitionTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('tuition:read_all') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TuitionOverviewDTO> getSystemOverview() {
        return ResponseEntity.ok(transactionService.getAdminOverview());
    }

    @GetMapping("/my-overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TuitionOverviewDTO> getMyOverview(Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        Long currentStudentId = userDetails.getUser().getId();

        return ResponseEntity.ok(transactionService.getStudentOverview(currentStudentId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('tuition:read_detail') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<TuitionOverviewDTO> getStudentOverviewByTeacher(@PathVariable Long studentId) {
        return ResponseEntity.ok(transactionService.getStudentOverview(studentId));
    }
}