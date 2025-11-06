package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "class_module")
public class ClassModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "module_id", nullable = false)
    private Integer moduleId;

    @Column(name = "schedule_type")
    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Class classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", insertable = false, updatable = false)
    private Module module;

    @OneToMany(mappedBy = "classModule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassModuleTeacher> teachers;

    @OneToMany(mappedBy = "classModule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassModuleSchedule> schedules;

    public enum ScheduleType {
        fixed, flexible, online, offline
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
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

    public Class getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(Class classEntity) {
        this.classEntity = classEntity;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public List<ClassModuleTeacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<ClassModuleTeacher> teachers) {
        this.teachers = teachers;
    }

    public List<ClassModuleSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ClassModuleSchedule> schedules) {
        this.schedules = schedules;
    }
}

