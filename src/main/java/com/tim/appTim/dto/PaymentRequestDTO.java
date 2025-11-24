package com.tim.appTim.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDTO {

    @NotNull(message = "ID đợt đóng tiền không được để trống")
    private Long scheduleId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    private String note;
}