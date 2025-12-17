package com.tim.appTim.controller;

import com.tim.appTim.dashboard.DashboardService;
import com.tim.appTim.dashboard.dto.DashboardJobLeadsResponse;
import com.tim.appTim.dashboard.dto.DashboardPendingResponse;
import com.tim.appTim.dashboard.dto.DashboardStatsResponse;
import com.tim.appTim.dashboard.dto.GrowthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<DashboardPendingResponse> getPendingRequests() {
        return ResponseEntity.ok(dashboardService.getPendingRequests());
    }

    @GetMapping("/job-leads")
    public ResponseEntity<DashboardJobLeadsResponse> getJobLeads() {
        return ResponseEntity.ok(dashboardService.getJobLeads());
    }

    @GetMapping("/growth")
    public ResponseEntity<GrowthResponse> getStudentGrowth(@RequestParam(name = "months", defaultValue = "6") int months) {
        return ResponseEntity.ok(dashboardService.getStudentGrowth(months));
    }
}
