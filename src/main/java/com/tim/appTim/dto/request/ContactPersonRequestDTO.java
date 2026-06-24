package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import lombok.Data;

import java.time.LocalDate;

@Data
public class ContactPersonRequestDTO {
    private Long companyId;
    private String name;
    private String position;
    private String email;
    private String phone;
    private LocalDate dob;
}




