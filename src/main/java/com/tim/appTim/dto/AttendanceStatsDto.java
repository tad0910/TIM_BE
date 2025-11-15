package com.tim.appTim.dto;

public class AttendanceStatsDto {
    private Integer studentId;
    private String studentName;
    private Integer attendedCount;
    private Integer totalSessions;
    private Double attendanceRate;

    // --- Getter & Setter ---

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Integer getAttendedCount() {
        return attendedCount;
    }

    public void setAttendedCount(Integer attendedCount) {
        this.attendedCount = attendedCount;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}