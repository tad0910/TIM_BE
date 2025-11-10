package com.tim.appTim.dto;

public class ModuleSessionDTO {
    private Long id;
    private Integer moduleId;
    private Integer sessionNumber;
    private String title;
    private String content;

    public ModuleSessionDTO() {
    }

    public ModuleSessionDTO(Long id, Integer moduleId, Integer sessionNumber, String title, String content) {
        this.id = id;
        this.moduleId = moduleId;
        this.sessionNumber = sessionNumber;
        this.title = title;
        this.content = content;
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
}
