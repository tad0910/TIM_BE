package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AttendanceStatus status = AttendanceStatus.absent;

    @Column(name = "marked_by", nullable = false)
    private Integer markedBy;

    @Column(name = "marked_at")
    private LocalDateTime markedAt = LocalDateTime.now();

    @Column(name = "notes")
    private String notes;

    public enum AttendanceStatus {
        present ,absent ,late ,excused
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Integer getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(Integer markedBy) {
        this.markedBy = markedBy;
    }

    public LocalDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(LocalDateTime markedAt) {
        this.markedAt = markedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}