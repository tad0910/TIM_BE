package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobActivityDTO {
    private Long id;
    private Long jobLeadId;
    private String activityType;
    private String content;
    private LocalDate happenedAt;
    private LocalDateTime createdAt;
    private String salaryAmount;
    private String note;
    private String fileUrl;
}





