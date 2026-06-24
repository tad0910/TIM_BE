package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.math.BigDecimal;
import java.util.List;

public class GradebookDTO {

    private Long classModuleId;
    private String className;
    private String moduleName;
    private int currentPage;
    private long totalElements;
    private int totalPages;

    private List<StudentGradeRowDTO> students;

    private List<String> components;

    public Long getClassModuleId() {
        return classModuleId;
    }

    public void setClassModuleId(Long classModuleId) {
        this.classModuleId = classModuleId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<String> getComponents() {
        return components;
    }
    public void setComponents(List<String> components) {
        this.components = components;
    }

    public List<StudentGradeRowDTO> getStudents() {
        return students;
    }

    public void setStudents(List<StudentGradeRowDTO> students) {
        this.students = students;
    }


    public static class StudentGradeRowDTO {

        private Long studentId;
        private String studentName;
        private Long gradeId;

        private BigDecimal theoryScore;
        private BigDecimal practiceScore;

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

        public Long getGradeId() {
            return gradeId;
        }

        public void setGradeId(Long gradeId) {
            this.gradeId = gradeId;
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
    }
}





