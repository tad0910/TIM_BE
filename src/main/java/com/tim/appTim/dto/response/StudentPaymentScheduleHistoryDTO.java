package com.tim.appTim.dto.response;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


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





