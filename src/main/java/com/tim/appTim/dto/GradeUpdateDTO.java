package com.tim.appTim.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class GradeUpdateDTO {

    @NotNull(message = "Điểm mới không được để trống")
    private BigDecimal newScore;
    private String changeReason;

    public BigDecimal getNewScore() {
        return newScore;
    }
    public void setNewScore(BigDecimal newScore) {
        this.newScore = newScore;
    }
    public String getChangeReason() {
        return changeReason;
    }
    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
}