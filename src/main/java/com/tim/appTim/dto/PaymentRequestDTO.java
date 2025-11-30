package com.tim.appTim.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDTO {

    private Long scheduleId;

    @NotNull(message = "ID học viên không được để trống")
    private Long studentId;

    @NotNull(message = "Số tiền đóng không được để trống")
    private java.math.BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    private String note;
}