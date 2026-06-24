package com.tim.appTim.dto.response;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import com.tim.appTim.entity.GradeHistory;
import java.math.BigDecimal;
import java.time.Instant;

public class GradeHistoryDTO {

    private Long id;
    private BigDecimal oldScore;
    private BigDecimal newScore;

    private String componentChanged;

    private Instant changedAt;
    private String changedByUserName;

    public GradeHistoryDTO(GradeHistory history) {
        this.id = history.getId();
        this.oldScore = history.getOldScore();
        this.newScore = history.getNewScore();

        this.componentChanged = history.getComponentChanged();

        this.changedAt = history.getChangedAt();

        if (history.getChangedBy() != null) {
            this.changedByUserName = history.getChangedBy().getFirstName() + " " + history.getChangedBy().getLastName();
        } else {
            this.changedByUserName = "Không rõ";
        }
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public BigDecimal getOldScore() {
        return oldScore;
    }
    public void setOldScore(BigDecimal oldScore) {
        this.oldScore = oldScore;
    }
    public BigDecimal getNewScore() {
        return newScore;
    }
    public void setNewScore(BigDecimal newScore) {
        this.newScore = newScore;
    }

    public String getComponentChanged() {
        return componentChanged;
    }
    public void setComponentChanged(String componentChanged) {
        this.componentChanged = componentChanged;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
    public String getChangedByUserName() {
        return changedByUserName;
    }
    public void setChangedByUserName(String changedByUserName) {
        this.changedByUserName = changedByUserName;
    }
}




