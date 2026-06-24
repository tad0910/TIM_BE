package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


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




