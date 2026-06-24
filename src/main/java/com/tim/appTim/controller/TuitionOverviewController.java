package com.tim.appTim.controller;

import com.tim.appTim.dto.response.TuitionOverviewDTO;
import com.tim.appTim.dto.common.TuitionTransactionDTO;
import com.tim.appTim.dto.common.StudentPaymentScheduleDTO;
import com.tim.appTim.service.TuitionTransactionService;
import com.tim.appTim.service.UserDetailsImpl;
import com.tim.appTim.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tuition-overview")
public class TuitionOverviewController {

    private final TuitionTransactionService transactionService;
    private final UserService userService;

    public TuitionOverviewController(TuitionTransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('tuition:read_all') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TuitionOverviewDTO> getSystemOverview() {
        return ResponseEntity.ok(transactionService.getAdminOverview());
    }

    @GetMapping("/my-overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TuitionOverviewDTO> getMyOverview(Authentication authentication) {

        Long currentStudentId = extractCurrentStudentId(authentication);
        if (currentStudentId == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(transactionService.getStudentOverview(currentStudentId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('tuition:read_detail') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<TuitionOverviewDTO> getStudentOverviewByTeacher(@PathVariable Long studentId) {
        return ResponseEntity.ok(transactionService.getStudentOverview(studentId));
    }

    @GetMapping("/student/{studentId}/schedules")
    @PreAuthorize("hasAuthority('tuition:read_detail') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<List<StudentPaymentScheduleDTO>> getStudentSchedules(@PathVariable Long studentId) {
        return ResponseEntity.ok(transactionService.getStudentSchedules(studentId));
    }

    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TuitionTransactionDTO>> getMyHistory(
            Authentication authentication,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        Long currentStudentId = extractCurrentStudentId(authentication);
        if (currentStudentId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(transactionService.getTransactionHistory(currentStudentId, pageable));
    }

    @GetMapping("/student/{studentId}/history")
    @PreAuthorize("hasAnyAuthority('tuition:read_detail', 'ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<Page<TuitionTransactionDTO>> getStudentHistory(
            @PathVariable Long studentId,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {

        return ResponseEntity.ok(transactionService.getTransactionHistory(studentId, pageable));
    }

    @GetMapping("/admin/transactions")
    @PreAuthorize("hasAuthority('tuition:read_all') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<TuitionTransactionDTO>> getAllTransactions(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    private Long extractCurrentStudentId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }

        if (principal instanceof UserDetailsImpl userDetailsImpl) {
            return userDetailsImpl.getUser() != null ? userDetailsImpl.getUser().getId() : null;
        }

        String usernameOrEmail = null;
        if (principal instanceof UserDetails userDetails) {
            usernameOrEmail = userDetails.getUsername();
        } else if (principal instanceof Jwt jwt) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            usernameOrEmail = preferredUsername != null ? preferredUsername : jwt.getSubject();
        } else {
            String s = principal.toString();
            usernameOrEmail = (s != null && !s.trim().isEmpty()) ? s : null;
        }

        if (usernameOrEmail == null) {
            return null;
        }

        Object loaded = userService.loadUserByUsername(usernameOrEmail);
        if (loaded instanceof UserDetailsImpl userDetailsImpl) {
            return userDetailsImpl.getUser() != null ? userDetailsImpl.getUser().getId() : null;
        }

        return null;
    }
}
