package com.tim.appTim.dto;

import java.time.LocalDateTime;

public class AttendanceDetailDto {

    private Long studentId;
    private String studentName;
    private String status; 
    private LocalDateTime markedAt; 
    private String notes;
    private Integer markedBy; 

    public AttendanceDetailDto() {
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(Integer markedBy) {
        this.markedBy = markedBy;
    }
}