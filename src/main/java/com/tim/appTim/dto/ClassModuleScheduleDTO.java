package com.tim.appTim.dto;

import com.tim.appTim.entity.ClassModuleSchedule.ScheduleStatus;
import java.time.LocalDateTime;

public class ClassModuleScheduleDTO {
    private Long id;
    private Long classId;
    private String className;
    private Integer moduleId;
    private String moduleName;
    private Long classModuleId;
    private Long moduleSessionId;
    private Long instructorId;
    private String instructorName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ScheduleStatus status;


    public ClassModuleScheduleDTO() {
    }

    public ClassModuleScheduleDTO(Long id, Long classId, String className, Integer moduleId, String moduleName,
                                  Long classModuleId, Long moduleSessionId, Long instructorId, String instructorName, 
                                  LocalDateTime startDate, LocalDateTime endDate, ScheduleStatus status) {
        this.id = id;
        this.classId = classId;
        this.className = className;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.classModuleId = classModuleId;
        this.moduleSessionId = moduleSessionId;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public Long getClassModuleId() {
        return classModuleId;
    }

    public void setClassModuleId(Long classModuleId) {
        this.classModuleId = classModuleId;
    }

    public Long getModuleSessionId() {
        return moduleSessionId;
    }

    public void setModuleSessionId(Long moduleSessionId) {
        this.moduleSessionId = moduleSessionId;
    }
}