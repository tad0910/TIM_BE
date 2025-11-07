package com.tim.appTim.dto;

import java.util.List;

public class ModuleDTO {
    private Integer id;
    private String name;
    private String description;
    private Integer position; 
    private List<ModuleSessionDTO> sessions;

    public ModuleDTO() {
    }

    public ModuleDTO(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public ModuleDTO(Integer id, String name, String description, Integer position) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.position = position;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public List<ModuleSessionDTO> getSessions() {
        return sessions;
    }

    public void setSessions(List<ModuleSessionDTO> sessions) {
        this.sessions = sessions;
    }
}
