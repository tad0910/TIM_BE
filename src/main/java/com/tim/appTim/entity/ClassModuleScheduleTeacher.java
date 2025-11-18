package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_module_schedule_teacher")
public class ClassModuleScheduleTeacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_module_schedule_id", nullable = false)
    private Long classModuleScheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private ScheduleTeacherRole role;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_module_schedule_id", insertable = false, updatable = false)
    private ClassModuleSchedule classModuleSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public enum ScheduleTeacherRole {
        TEACHER
    }

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassModuleScheduleId() {
        return classModuleScheduleId;
    }

    public void setClassModuleScheduleId(Long classModuleScheduleId) {
        this.classModuleScheduleId = classModuleScheduleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ScheduleTeacherRole getRole() {
        return role;
    }

    public void setRole(ScheduleTeacherRole role) {
        this.role = role;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public ClassModuleSchedule getClassModuleSchedule() {
        return classModuleSchedule;
    }

    public void setClassModuleSchedule(ClassModuleSchedule classModuleSchedule) {
        this.classModuleSchedule = classModuleSchedule;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

