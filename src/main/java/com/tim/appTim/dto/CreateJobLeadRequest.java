package com.tim.appTim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobLeadRequest {
    private String companyName;
    private String shortName;
    private String address;
    private String website;
}
