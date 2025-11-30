package com.tim.appTim.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class CompanyRequestDTO {
    @NotBlank(message = "Tên không được để trống")
    private String name;
    private String shortName;
    private String type;
    private Set<String> technologies;
    private Set<String> regions;
    private Set<String> markets;
    private String introduction;
    private String website;
    private String phone;
    private String size;

    private Set<String> products;
    private String address;
    private String profileUrl;
    private String benefits;
    private LocalDate foundingDate;
}