package com.tim.appTim.dto;

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
    private String status;

    private boolean isFromAdmin;
    private LocalDateTime date;
}