package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleDueDateUpdateDTO {

    @NotNull(message = "Ngày hạn mới không được để trống")
    private LocalDate dueDate;

    private String reason;
}





