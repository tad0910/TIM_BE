package com.tim.appTim.dto;

import com.tim.appTim.entity.Grade;
import java.math.BigDecimal;
import java.time.LocalDate;

public class GradeDTO {

    private Long id;

    private Long studentId;
    private String studentName;

    private Long classModuleId;
    private String moduleName;

    private BigDecimal theoryScore;
    private BigDecimal practiceScore;

    private LocalDate entryDate;

    public GradeDTO() {
    }

    public GradeDTO(Grade grade) {
        this.id = grade.getId();
        this.classModuleId = grade.getClassModule().getId();
        this.moduleName = grade.getClassModule().getModule().getName();

        this.studentId = grade.getStudent().getId();
        this.studentName = grade.getStudent().getFirstName() + " " + grade.getStudent().getLastName();

        this.theoryScore = grade.getTheoryScore();
        this.practiceScore = grade.getPracticeScore();
        this.entryDate = grade.getEntryDate();
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getStudentId() {
        return studentId;
    }
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
    public String getStudentName() {
        return studentName;
    }
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    public Long getClassModuleId() {
        return classModuleId;
    }
    public void setClassModuleId(Long classModuleId) {
        this.classModuleId = classModuleId;
    }
    public String getModuleName() {
        return moduleName;
    }
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
    public BigDecimal getTheoryScore() {
        return theoryScore;
    }
    public void setTheoryScore(BigDecimal theoryScore) {
        this.theoryScore = theoryScore;
    }
    public BigDecimal getPracticeScore() {
        return practiceScore;
    }
    public void setPracticeScore(BigDecimal practiceScore) {
        this.practiceScore = practiceScore;
    }
    public LocalDate getEntryDate() {
        return entryDate;
    }
    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }
}