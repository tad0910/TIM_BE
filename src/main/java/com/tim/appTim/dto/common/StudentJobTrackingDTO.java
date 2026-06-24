package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


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




