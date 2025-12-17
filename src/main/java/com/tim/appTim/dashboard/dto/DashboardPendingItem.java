package com.tim.appTim.dashboard.dto;

public record DashboardPendingItem(
        String title,
        String description,
        String type,
        String status
) {
}
