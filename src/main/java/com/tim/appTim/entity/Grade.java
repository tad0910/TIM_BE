package com.tim.appTim.entity; 

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "grades", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "class_module_id"})
})
@SQLDelete(sql = "UPDATE grades SET status = 'DELETED' WHERE id = ?")
@Where(clause = "status = 'ACTIVE'")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_module_id", nullable = false)
    private ClassModule classModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "theory_score")
    private BigDecimal theoryScore;

    @Column(name = "practice_score")
    private BigDecimal practiceScore;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entered_by_user_id")
    private User enteredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Status {
        ACTIVE,
        DELETED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ClassModule getClassModule() { return classModule; }
    public void setClassModule(ClassModule classModule) { this.classModule = classModule; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public BigDecimal getTheoryScore() { return theoryScore; }
    public void setTheoryScore(BigDecimal theoryScore) { this.theoryScore = theoryScore; }
    public BigDecimal getPracticeScore() { return practiceScore; }
    public void setPracticeScore(BigDecimal practiceScore) { this.practiceScore = practiceScore; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public User getEnteredBy() { return enteredBy; }
    public void setEnteredBy(User enteredBy) { this.enteredBy = enteredBy; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}