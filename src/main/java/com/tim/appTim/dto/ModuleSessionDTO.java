package com.tim.appTim.dto;

import java.time.LocalDateTime;

public class ModuleSessionDTO {
    private Long id;
    private Integer moduleId;
    private Integer sessionNumber;
    private String title;
    private String content;
    private LocalDateTime scheduledAt;
    private LocalDateTime endDate;
    private String status;
    private Long instructorId;
    private String instructorName;

    public ModuleSessionDTO() {
    }

    public ModuleSessionDTO(Long id, Integer moduleId, Integer sessionNumber, String title, String content, LocalDateTime scheduledAt) {
        this.id = id;
        this.moduleId = moduleId;
        this.sessionNumber = sessionNumber;
        this.title = title;
        this.content = content;
        this.scheduledAt = scheduledAt;
    }

    public ModuleSessionDTO(Long id, Integer moduleId, Integer sessionNumber, String title, String content, LocalDateTime scheduledAt, LocalDateTime endDate, String status) {
        this.id = id;
        this.moduleId = moduleId;
        this.sessionNumber = sessionNumber;
        this.title = title;
        this.content = content;
        this.scheduledAt = scheduledAt;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public String getInstructorName() {
        return instructorName;
    }

    public void setInstructorName(String instructorName) {
        this.instructorName = instructorName;
    }
}
