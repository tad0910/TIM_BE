package com.tim.appTim.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class MarkAttendanceRequest {
    @NotNull
    private Integer teacherId;

    @NotEmpty
    private List<AttendanceMarkDto> records;

    // --- Getter & Setter ---

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public List<AttendanceMarkDto> getRecords() {
        return records;
    }

    public void setRecords(List<AttendanceMarkDto> records) {
        this.records = records;
    }
}