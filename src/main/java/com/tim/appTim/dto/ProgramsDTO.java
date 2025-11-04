package com.tim.appTim.dto;

import java.util.List;

public class ProgramsDTO {
    private Integer id;
    private String name;
    private String description;
    private List<ModuleDTO> modules;

    public ProgramsDTO() {
    }

    public ProgramsDTO(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public ProgramsDTO(Integer id, String name, String description, List<ModuleDTO> modules) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.modules = modules;
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

    public List<ModuleDTO> getModules() {
        return modules;
    }

    public void setModules(List<ModuleDTO> modules) {
        this.modules = modules;
    }
}
