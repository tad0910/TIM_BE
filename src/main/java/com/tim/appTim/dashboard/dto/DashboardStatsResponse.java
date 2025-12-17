package com.tim.appTim.dashboard.dto;

public record DashboardStatsResponse(
        int students,
        int pendingForms,
        int jobPending,
        int activeClasses,
        int activeMentors
) {
}
