package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.time.LocalDateTime;

public class UserClassDTO {
    private Long classId;
    private String className;
    private String description;
    private String role;
    private LocalDateTime joinDate;

    public UserClassDTO() {}

    public UserClassDTO(Long classId, String className, String description, String role, LocalDateTime joinDate) {
        this.classId = classId;
        this.className = className;
        this.description = description;
        this.role = role;
        this.joinDate = joinDate;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }
}





