package com.tim.appTim.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.tim.appTim.entity.StudentForm;

public class StudentFormResponseDTO {
    
    private Long id;
    private String templateName; 
    private Long studentId;
    private String studentName;
    private String phoneNumber;  
    private String email;
    private String className;
    private String programName; 
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal feeAmount;
    private StudentForm.ApprovalStatus coachApproval;
    private String coachNote;
    private String coachName; 
    private StudentForm.ApprovalStatus academicApproval;
    private String academicNote;
    private String academicName; 
    private StudentForm.ApprovalStatus accountantApproval;
    private String accountantNote;
    private String accountantName; 
    private StudentForm.ApprovalStatus adminApproval;
    private String adminNote;
    private String adminName;

    private StudentForm.FormStatus status;
    private LocalDateTime createdAt;

    public StudentFormResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }

    public StudentForm.ApprovalStatus getCoachApproval() { return coachApproval; }
    public void setCoachApproval(StudentForm.ApprovalStatus coachApproval) { this.coachApproval = coachApproval; }

    public String getCoachNote() { return coachNote; }
    public void setCoachNote(String coachNote) { this.coachNote = coachNote; }

    public String getCoachName() { return coachName; }
    public void setCoachName(String coachName) { this.coachName = coachName; }

    public StudentForm.ApprovalStatus getAcademicApproval() { return academicApproval; }
    public void setAcademicApproval(StudentForm.ApprovalStatus academicApproval) { this.academicApproval = academicApproval; }

    public String getAcademicNote() { return academicNote; }
    public void setAcademicNote(String academicNote) { this.academicNote = academicNote; }

    public String getAcademicName() { return academicName; }
    public void setAcademicName(String academicName) { this.academicName = academicName; }

    public StudentForm.ApprovalStatus getAccountantApproval() { return accountantApproval; }
    public void setAccountantApproval(StudentForm.ApprovalStatus accountantApproval) { this.accountantApproval = accountantApproval; }

    public String getAccountantNote() { return accountantNote; }
    public void setAccountantNote(String accountantNote) { this.accountantNote = accountantNote; }

    public String getAccountantName() { return accountantName; }
    public void setAccountantName(String accountantName) { this.accountantName = accountantName; }

    public StudentForm.ApprovalStatus getAdminApproval() { return adminApproval; }
    public void setAdminApproval(StudentForm.ApprovalStatus adminApproval) { this.adminApproval = adminApproval; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public StudentForm.FormStatus getStatus() { return status; }
    public void setStatus(StudentForm.FormStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}