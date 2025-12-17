package com.tim.appTim.dashboard.dto;

public record DashboardJobLead(
        String company,
        String position,
        String status,
        String mentor,
        String deadline
) {
}
