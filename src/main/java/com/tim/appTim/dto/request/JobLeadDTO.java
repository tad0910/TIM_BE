package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobLeadDTO {
    private Long id;
    private String companyName;
    private String shortName;
    private String address;
    private String website;
    private String statusCode;
    private String statusLabel;
    private String status;

    private boolean isFromAdmin;
    private LocalDateTime date;
}




