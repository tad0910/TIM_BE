package com.tim.appTim.dto;

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