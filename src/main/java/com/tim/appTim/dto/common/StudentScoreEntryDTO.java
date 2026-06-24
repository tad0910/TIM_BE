package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.math.BigDecimal;
import java.util.Map;

public class StudentScoreEntryDTO {

    private Long studentId;
    private Map<String, BigDecimal> components;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Map<String, BigDecimal> getComponents() { return components; }
    public void setComponents(Map<String, BigDecimal> components) { this.components = components; }
}




