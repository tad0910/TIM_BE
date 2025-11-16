package com.tim.appTim.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class GradebookDTO {

    private Long classModuleId;
    private String className;
    private String moduleName;
    private List<String> components;

    private List<StudentRow> students;
    private int currentPage;
    private long totalElements;
    private int totalPages;

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

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public List<StudentRow> getStudents() {
        return students;
    }

    public void setStudents(List<StudentRow> students) {
        this.students = students;
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public static class StudentRow {

        private Long studentId;
        private String studentName;
        private Map<String, BigDecimal> grades;

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

        public Map<String, BigDecimal> getGrades() {
            return grades;
        }

        public void setGrades(Map<String, BigDecimal> grades) {
            this.grades = grades;
        }
    }
}
