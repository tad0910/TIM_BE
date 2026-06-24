package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class FeeAdjustmentDTO {

    @NotNull(message = "ID hồ sơ học phí không được để trống")
    private Long studentTuitionId;

    @NotNull(message = "Số tiền điều chỉnh không được để trống")
    private BigDecimal amount;

    private boolean isFixedAmount;

    private String reason;
}




