package com.tim.appTim.entity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "modules")
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramModule> programModules;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModuleSession> moduleSessions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public List<ProgramModule> getProgramModules() {
        return programModules;
    }

    public void setProgramModules(List<ProgramModule> programModules) {
        this.programModules = programModules;
    }

    public List<ModuleSession> getModuleSessions() {
        return moduleSessions;
    }

    public void setModuleSessions(List<ModuleSession> moduleSessions) {
        this.moduleSessions = moduleSessions;
    }
}
