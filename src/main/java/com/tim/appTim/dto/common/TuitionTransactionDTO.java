package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import com.tim.appTim.entity.TuitionTransaction;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TuitionTransactionDTO {
    private Long id;
    private String transactionType;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String description;
    private String performedBy;
    private Long receiptId;
    private String receiptCode;

    public TuitionTransactionDTO(TuitionTransaction entity) {
        this.id = entity.getId();
        this.transactionType = entity.getType().name();
        this.amount = entity.getAmount();
        this.transactionDate = entity.getTransactionDate();
        this.description = entity.getDescription();

        if (entity.getPerformedBy() != null) {
            this.performedBy = entity.getPerformedBy().getUsername();
        } else {
            this.performedBy = "Hệ thống";
        }
    }
}




