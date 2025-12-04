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
public class JobTrackingRowDTO {
    private Long studentId;
    private String studentName;
    private String username;
    private String jobStatusCode;
    private String jobStatusLabel;
    private String companyName;
    private String offerAmount;
    private String probationSalary;
    private String officialSalary;
    private boolean jobInterest;
    private LocalDateTime lastUpdated;
}
