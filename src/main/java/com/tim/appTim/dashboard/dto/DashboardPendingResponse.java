package com.tim.appTim.dashboard.dto;

import java.util.List;

public record DashboardPendingResponse(
        List<DashboardPendingItem> items
) {
}
