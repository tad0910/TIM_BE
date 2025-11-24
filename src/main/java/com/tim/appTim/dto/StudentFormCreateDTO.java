package com.tim.appTim.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StudentFormCreateDTO {
    
    private Long templateId;     
    private Long studentId;      
    private Long classId;       
    
    private String fullName;     
    private String phoneNumber;  
    private String email;

    private String reason;        
    private LocalDate startDate;  
    private LocalDate endDate;   
    private BigDecimal feeAmount; 
    
    private Long targetClassId;      
    private String targetProgramType; 

    public StudentFormCreateDTO() {
    }

    
    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getFullName() {
        return fullName;
    }
    
    public void setStudentName(String studentName) {
        this.fullName = studentName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public Long getTargetClassId() {
        return targetClassId;
    }

    public void setTargetClassId(Long targetClassId) {
        this.targetClassId = targetClassId;
    }

    public String getTargetProgramType() {
        return targetProgramType;
    }

    public void setTargetProgramType(String targetProgramType) {
        this.targetProgramType = targetProgramType;
    }
}