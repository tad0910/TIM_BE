package com.tim.appTim.dashboard.dto;

import java.util.List;

public record DashboardJobLeadsResponse(
        List<DashboardJobLead> items
) {
}
