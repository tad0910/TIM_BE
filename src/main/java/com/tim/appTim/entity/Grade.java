package com.tim.appTim.entity; // Đảm bảo đúng package

import jakarta.persistence.*; // Hoặc javax.persistence.* nếu dùng Spring Boot 2
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "grades") // Ánh xạ với bảng CSDL 'grades'
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_module_id", nullable = false)
    private ClassModule classModule; // Tương ứng 'class_module_id'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student; // Tương ứng 'student_id'

    @Column(name = "component_name", nullable = false)
    private String componentName; // Tương ứng 'component_name'

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score; // Tương ứng 'score'

    @Column(name = "max_score", precision = 5, scale = 2)
    private BigDecimal maxScore; // Tương ứng 'max_score'

    @Column(name = "weight_percent", precision = 5, scale = 4)
    private BigDecimal weightPercent; // Tương ứng 'weight_percent'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by_user_id")
    private User enteredBy; // Tương ứng 'entered_by_user_id'

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // Tự động set thời gian
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- Getters và Setters (BẮT BUỘC) ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClassModule getClassModule() {
        return classModule;
    }

    public void setClassModule(ClassModule classModule) {
        this.classModule = classModule;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getWeightPercent() {
        return weightPercent;
    }

    public void setWeightPercent(BigDecimal weightPercent) {
        this.weightPercent = weightPercent;
    }

    public User getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(User enteredBy) {
        this.enteredBy = enteredBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}