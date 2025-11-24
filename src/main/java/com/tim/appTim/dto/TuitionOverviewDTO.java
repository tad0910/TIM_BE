package com.tim.appTim.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TuitionOverviewDTO {
    private BigDecimal totalPaid;
    private BigDecimal totalRefunded;
    private BigDecimal totalException;
    private BigDecimal totalUsed;

    private BigDecimal currentBalance;
}