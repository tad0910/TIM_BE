package com.tim.appTim.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class JobActivityRequest {
    private Long jobLeadId;

    private String activityType;

    private String content;

    private LocalDate happenedAt;

    private String salaryAmount;

}