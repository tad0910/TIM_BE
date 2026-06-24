package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import com.tim.appTim.entity.TuitionRoute;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TuitionRouteDTO {
    private Long id;

    @NotNull(message = "Phải chọn chương trình học cho lộ trình này")
    private Integer programId;

    @NotBlank(message = "Tên lộ trình không được để trống")
    private String name;

    @NotNull(message = "Loại lộ trình là bắt buộc")
    private TuitionRoute.TuitionRouteType type;

    @Min(value = 0, message = "Phí nhập học phải lớn hơn hoặc bằng 0")
    private BigDecimal admissionFee;

    @Min(value = 0, message = "Học phí tháng đầu phải lớn hơn hoặc bằng 0")
    private BigDecimal firstMonthFee;

    @Min(value = 0, message = "Tổng học phí phải lớn hơn hoặc bằng 0")
    private BigDecimal totalListedFee;

    @Min(value = 1, message = "Số đợt phải ít nhất là 1")
    private Integer numberOfInstallments;

    @Min(value = 1, message = "Tần suất phải ít nhất là 1 tháng")
    private Integer frequency;

    private String description;

    @Valid
    private List<InstallmentConfigDTO> installmentConfigs;
}




