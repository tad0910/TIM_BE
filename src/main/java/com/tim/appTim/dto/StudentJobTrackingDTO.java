package com.tim.appTim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentJobTrackingDTO {
    private Long studentId;
    private String studentName;
    private String jobStatus;
    private String companyName;
    private String offerAmount;
    private String probationSalary;
    private String officialSalary;
    private boolean isJobSeeking;
}