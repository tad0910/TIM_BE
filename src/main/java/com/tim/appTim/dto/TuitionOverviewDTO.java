package com.tim.appTim.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TuitionOverviewDTO {
    private BigDecimal totalPaid;
    private BigDecimal totalRefunded;
    private BigDecimal totalException;
    private BigDecimal totalUsed;

    private BigDecimal currentBalance;

    private BigDecimal totalWaived;
    
    // Constructor for queries that only pass 5 parameters (without totalWaived)
    public TuitionOverviewDTO(BigDecimal totalPaid,
                               BigDecimal totalRefunded,
                               BigDecimal totalException,
                               BigDecimal totalUsed,
                               BigDecimal currentBalance) {
        this.totalPaid = totalPaid;
        this.totalRefunded = totalRefunded;
        this.totalException = totalException;
        this.totalUsed = totalUsed;
        this.currentBalance = currentBalance;
        this.totalWaived = BigDecimal.ZERO;
    }
}