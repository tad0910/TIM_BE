package com.tim.appTim.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ReceiptDTO {
    private String companyName;
    private String companyAddress;

    private String receiptId;
    private LocalDate paymentDate;

    private String payerName;
    private String payerAddress;
    private String paymentReason;

    private String amountNumber;
    private String amountInWords;

    private String attachment;
}