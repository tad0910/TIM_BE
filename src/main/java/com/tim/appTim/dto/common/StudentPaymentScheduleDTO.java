package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import com.tim.appTim.entity.StudentPaymentSchedule;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class StudentPaymentScheduleDTO {
    private Long id;
    private Long studentTuitionId;
    private Integer installmentNumber;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private LocalDate fromDate;
    private LocalDate dueDate;
    private String status; 

    public StudentPaymentScheduleDTO(StudentPaymentSchedule entity) {
        this.id = entity.getId();
        this.studentTuitionId = entity.getStudentTuition() != null ? entity.getStudentTuition().getId() : null;
        this.installmentNumber = entity.getInstallmentNumber();
        this.expectedAmount = entity.getExpectedAmount();
        this.paidAmount = entity.getPaidAmount();
        this.fromDate = entity.getFromDate();
        this.dueDate = entity.getDueDate();
        this.status = entity.getStatus() != null ? entity.getStatus().name() : "UNKNOWN";
    }
}





