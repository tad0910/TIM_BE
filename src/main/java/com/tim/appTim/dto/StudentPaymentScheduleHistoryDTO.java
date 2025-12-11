package com.tim.appTim.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentPaymentScheduleHistoryDTO(
        Long id,
        Long scheduleId,
        LocalDate oldDueDate,
        LocalDate newDueDate,
        String reason,
        Long modifiedByUserId,
        String modifiedByUsername,
        LocalDateTime createdAt
) {
}
