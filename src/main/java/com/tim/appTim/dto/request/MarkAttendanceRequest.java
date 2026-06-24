package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class MarkAttendanceRequest {
    @NotNull
    private Integer teacherId;

    @NotEmpty
    private List<AttendanceMarkDto> records;

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




