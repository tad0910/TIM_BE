package com.tim.appTim.dto;

import com.tim.appTim.entity.GradeHistory;
import java.math.BigDecimal;
import java.time.Instant;

public class GradeHistoryDTO {

    private Long id;
    private BigDecimal oldScore;
    private BigDecimal newScore;

    // SỬA: Thay "changeReason" bằng "componentChanged"
    private String componentChanged; // (Ví dụ: "Điểm lý thuyết", "Điểm thực hành", "entry_date")

    private Instant changedAt;
    private String changedByUserName;

    // SỬA: Cập nhật Constructor
    public GradeHistoryDTO(GradeHistory history) {
        this.id = history.getId();
        this.oldScore = history.getOldScore();
        this.newScore = history.getNewScore();

        // SỬA: Lấy từ hàm mới của Entity
        this.componentChanged = history.getComponentChanged();

        this.changedAt = history.getChangedAt();

        if (history.getChangedBy() != null) {
            this.changedByUserName = history.getChangedBy().getFirstName() + " " + history.getChangedBy().getLastName();
        } else {
            this.changedByUserName = "Không rõ";
        }
    }

    // --- Getters & Setters (Đã cập nhật) ---

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

    // SỬA: Getter/Setter cho componentChanged
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