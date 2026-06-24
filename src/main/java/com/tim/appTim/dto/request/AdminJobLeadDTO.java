package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminJobLeadDTO {

    private Long id;
    private String companyName;
    private String shortName;
    private String address;
    private String website;

    private String statusCode;
    private String statusLabel;
    private LocalDateTime createdAt;
    private boolean fromAdmin;

    private List<JobActivityDTO> activities;
}





