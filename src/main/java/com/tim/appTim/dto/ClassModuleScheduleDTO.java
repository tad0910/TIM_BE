package com.tim.appTim.dto;

import com.tim.appTim.entity.ClassModuleSchedule.ScheduleStatus;
import java.time.LocalDate;

public class ClassModuleScheduleDTO {
    private Long id;
    private Long classId;
    private String className;
    private Long moduleId;
    private String moduleName;
    private Long instructorId;
    private String instructorName;
    private LocalDate startDate;
    private LocalDate endDate;
    private ScheduleStatus status;


    public ClassModuleScheduleDTO() {
    }

    public ClassModuleScheduleDTO(Long id, Long classId, String className, Long moduleId, String moduleName,
                                  Long instructorId, String instructorName, LocalDate startDate, LocalDate endDate,
                                  ScheduleStatus status) {
        this.id = id;
        this.classId = classId;
        this.className = className;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
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

    public Long getModuleId() {
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
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

    public void setModuleId(Long moduleId) {
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

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }
}