package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InstallmentConfigDTO {

    @NotNull(message = "Số kỳ không được để trống")
    @Min(value = 1, message = "Số kỳ phải >= 1")
    private Integer installmentNumber;

    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 0, message = "Số tiền phải >= 0")
    private BigDecimal baseAmount;

    @NotNull(message = "Số ngày không được để trống")
    @Min(value = 0, message = "Số ngày phải >= 0")
    private Integer daysFromPrevious;
}





