package com.tim.appTim.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTrackingOverviewSummaryDTO {
    private Integer totalClasses;
    private Integer totalStudents;
    private Integer totalOffers;
    private Integer activeJobInterest;
    private Integer updatedWithin14Days;
    private Integer recentUpdatePercent;

    private List<JobTrackingOverviewClassDTO> classes;
}
