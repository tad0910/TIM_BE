package com.tim.appTim.dto.response;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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




