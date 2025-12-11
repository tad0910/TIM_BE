package com.tim.appTim.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_payment_schedule_history")
public class StudentPaymentScheduleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private StudentPaymentSchedule schedule;

    @Column(name = "old_due_date")
    private LocalDate oldDueDate;

    @Column(name = "new_due_date", nullable = false)
    private LocalDate newDueDate;

    @Column(name = "reason")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by_user_id")
    private User modifiedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public StudentPaymentSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(StudentPaymentSchedule schedule) {
        this.schedule = schedule;
    }

    public LocalDate getOldDueDate() {
        return oldDueDate;
    }

    public void setOldDueDate(LocalDate oldDueDate) {
        this.oldDueDate = oldDueDate;
    }

    public LocalDate getNewDueDate() {
        return newDueDate;
    }

    public void setNewDueDate(LocalDate newDueDate) {
        this.newDueDate = newDueDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(User modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
