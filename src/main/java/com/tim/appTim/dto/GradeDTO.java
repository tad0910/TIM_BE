package com.tim.appTim.dto; // Đảm bảo đúng package

import com.tim.appTim.entity.Grade; // Import Entity 'Grade' mới
import java.math.BigDecimal;
import java.time.LocalDate;

public class GradeDTO {

    private Long id; // ID của chính bản ghi điểm

    // Thông tin sinh viên
    private Long studentId;
    private String studentName;

    // Thông tin môn học
    private Long classModuleId;
    private String moduleName;

    // Các cột điểm mới
    private BigDecimal theoryScore;   // Điểm lý thuyết
    private BigDecimal practiceScore; // Điểm thực hành

    // Thông tin ngày (từ UI "Chọn ngày")
    private LocalDate entryDate;

    // Constructor rỗng
    public GradeDTO() {
    }

    // Constructor để map (ánh xạ) từ Entity
    public GradeDTO(Grade grade) {
        this.id = grade.getId();
        this.classModuleId = grade.getClassModule().getId();
        this.moduleName = grade.getClassModule().getModule().getName(); // Lấy tên môn

        this.studentId = grade.getStudent().getId();
        this.studentName = grade.getStudent().getFirstName() + " " + grade.getStudent().getLastName();

        this.theoryScore = grade.getTheoryScore();
        this.practiceScore = grade.getPracticeScore();
        this.entryDate = grade.getEntryDate();
    }

    // --- Getters và Setters ---

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