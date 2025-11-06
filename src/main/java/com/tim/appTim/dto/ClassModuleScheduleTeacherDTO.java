package com.tim.appTim.dto;

import com.tim.appTim.entity.ClassModuleScheduleTeacher.ScheduleTeacherRole;
import java.time.LocalDateTime;

public class ClassModuleScheduleTeacherDTO {
    private Long id;
    private Long classModuleScheduleId;
    private Long userId;
    private String userName;
    private String userEmail;
    private ScheduleTeacherRole role;
    private LocalDateTime assignedAt;

    public ClassModuleScheduleTeacherDTO() {
    }

    public ClassModuleScheduleTeacherDTO(Long id, Long classModuleScheduleId, Long userId, 
                                        String userName, String userEmail, ScheduleTeacherRole role, 
                                        LocalDateTime assignedAt) {
        this.id = id;
        this.classModuleScheduleId = classModuleScheduleId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.role = role;
        this.assignedAt = assignedAt;
    }

    // Getters and Setters
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
}

