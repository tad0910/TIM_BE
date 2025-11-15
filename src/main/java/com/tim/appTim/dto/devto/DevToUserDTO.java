package com.tim.appTim.dto.devto;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class DevToUserDTO {

    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}