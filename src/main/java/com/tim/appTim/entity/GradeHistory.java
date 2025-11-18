package com.tim.appTim.entity; // Đảm bảo đúng package

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "grade_history")
public class GradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade; // Liên kết tới 'grades' (bảng mới)

    // --- CỘT MỚI (THEO YÊU CẦU) ---
    @Column(name = "component_changed", nullable = false)
    private String componentChanged; // Tên cột đã thay đổi (ví dụ: "theory_score")

    @Column(name = "old_score")
    private BigDecimal oldScore;

    @Column(name = "new_score")
    private BigDecimal newScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", updatable = false)
    private Instant changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = Instant.now();
    }

    // --- Getters và Setters ---
    // (Bắt buộc)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Grade getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = grade; }
    public String getComponentChanged() { return componentChanged; }
    public void setComponentChanged(String componentChanged) { this.componentChanged = componentChanged; }
    public BigDecimal getOldScore() { return oldScore; }
    public void setOldScore(BigDecimal oldScore) { this.oldScore = oldScore; }
    public BigDecimal getNewScore() { return newScore; }
    public void setNewScore(BigDecimal newScore) { this.newScore = newScore; }
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}