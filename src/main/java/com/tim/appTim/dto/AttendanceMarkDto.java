package com.tim.appTim.dto;

import jakarta.validation.constraints.NotNull;

public class AttendanceMarkDto {
    @NotNull
    private Integer studentId;

    @NotNull
 private String status;

    private String notes;

    // --- Getter & Setter ---

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}