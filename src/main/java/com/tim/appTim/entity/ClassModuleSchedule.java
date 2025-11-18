package com.tim.appTim.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "class_module_schedules")
public class ClassModuleSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "module_id")
    private Integer moduleId;

    @Column(name = "class_module_id")
    private Long classModuleId;

    @Column(name = "module_session_id")
    private Long moduleSessionId;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status;

    @Column(name = "instructor_id")
    private Long instructorId;

    public enum ScheduleStatus {
        planned, ongoing, completed
    }

    @ManyToOne
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private Class classEntity;

    @ManyToOne
    @JoinColumn(name = "module_id", insertable = false, updatable = false)
    private Module module;

    @ManyToOne
    @JoinColumn(name = "class_module_id", insertable = false, updatable = false)
    private ClassModule classModule;

    @ManyToOne
    @JoinColumn(name = "module_session_id", insertable = false, updatable = false)
    private ModuleSession moduleSession;

    @OneToMany(mappedBy = "classModuleSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ClassModuleScheduleTeacher> teachers;

    @ManyToOne
    @JoinColumn(name = "instructor_id", insertable = false, updatable = false)
    private User instructor;

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

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
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

    public User getInstructor() {
        return instructor;
    }

    public void setInstructor(User instructor) {
        this.instructor = instructor;
    }

    public ClassModule getClassModule() {
        return classModule;
    }

    public void setClassModule(ClassModule classModule) {
        this.classModule = classModule;
    }

    public ModuleSession getModuleSession() {
        return moduleSession;
    }

    public void setModuleSession(ModuleSession moduleSession) {
        this.moduleSession = moduleSession;
    }

    public java.util.List<ClassModuleScheduleTeacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(java.util.List<ClassModuleScheduleTeacher> teachers) {
        this.teachers = teachers;
    }
}
