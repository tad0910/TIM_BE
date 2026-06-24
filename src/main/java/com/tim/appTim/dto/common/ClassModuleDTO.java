package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import com.tim.appTim.entity.ClassModule.ScheduleType;
import java.time.LocalDateTime;
import java.util.List;

public class ClassModuleDTO {
    private Long id;
    private Long classId;
    private String className;
    private Integer moduleId;
    private String moduleName;
    private ScheduleType scheduleType;
    private LocalDateTime createdAt;
    private List<ClassModuleTeacherDTO> teachers;

    public ClassModuleDTO() {
    }

    public ClassModuleDTO(Long id, Long classId, String className, Integer moduleId, String moduleName, 
                         ScheduleType scheduleType, LocalDateTime createdAt) {
        this.id = id;
        this.classId = classId;
        this.className = className;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.scheduleType = scheduleType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ClassModuleTeacherDTO> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<ClassModuleTeacherDTO> teachers) {
        this.teachers = teachers;
    }
}






