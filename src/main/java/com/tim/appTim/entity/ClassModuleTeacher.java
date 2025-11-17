package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "class_module_teacher")
public class ClassModuleTeacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_module_id", nullable = false)
    private Long classModuleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private TeacherRole role;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_module_id", insertable = false, updatable = false)
    private ClassModule classModule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public enum TeacherRole {
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

    public Long getClassModuleId() {
        return classModuleId;
    }

    public void setClassModuleId(Long classModuleId) {
        this.classModuleId = classModuleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public TeacherRole getRole() {
        return role;
    }

    public void setRole(TeacherRole role) {
        this.role = role;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public ClassModule getClassModule() {
        return classModule;
    }

    public void setClassModule(ClassModule classModule) {
        this.classModule = classModule;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

