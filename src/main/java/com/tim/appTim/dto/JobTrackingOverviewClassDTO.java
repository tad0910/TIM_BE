package com.tim.appTim.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTrackingOverviewClassDTO {
    private Long classId;
    private String className;
    private Integer programId;
    private String programName;

    private Integer totalStudents;
    private Integer totalLeads;
    private Integer offerCount;
    private Integer activeJobInterest;
    private Integer updatedWithin14Days;
    private Integer recentUpdatePercent;
    private LocalDateTime lastActivityAt;
}
