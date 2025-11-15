package com.tim.appTim.dto; // Đảm bảo đúng package

import java.math.BigDecimal;

public class GradeUpdateDTO {

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