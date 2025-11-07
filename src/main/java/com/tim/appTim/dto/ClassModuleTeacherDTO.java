package com.tim.appTim.dto;

import com.tim.appTim.entity.ClassModuleTeacher.TeacherRole;
import java.time.LocalDateTime;

public class ClassModuleTeacherDTO {
    private Long id;
    private Long classModuleId;
    private Long userId;
    private String userName;
    private String userEmail;
    private TeacherRole role;
    private LocalDateTime assignedAt;

    public ClassModuleTeacherDTO() {
    }

    public ClassModuleTeacherDTO(Long id, Long classModuleId, Long userId, String userName, 
                                 String userEmail, TeacherRole role, LocalDateTime assignedAt) {
        this.id = id;
        this.classModuleId = classModuleId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.role = role;
        this.assignedAt = assignedAt;
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
}

