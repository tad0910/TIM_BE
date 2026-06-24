package com.tim.appTim.dto.response;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


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





