package com.tim.appTim.entity;

import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "classes")
public class Class {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String className;

    @Column(name = "description")
    private String description;

    @Column(name = "program_id")
    private Integer programId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", insertable = false, updatable = false)
    private Programs program;

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassModuleSchedule> schedules;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<ClassModuleSchedule> getSchedules() { return schedules; }
    public void setSchedules(List<ClassModuleSchedule> schedules) { this.schedules = schedules;}
    
    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public Programs getProgram() {
        return program;
    }

    public void setProgram(Programs program) {
        this.program = program;
    }
}