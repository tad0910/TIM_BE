package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime; 

@Entity
@Table(name = "student_forms")
public class StudentForm { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private FormTemplate template;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "so_dien_thoai")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private Class classRoom;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal feeAmount;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus coachApproval = ApprovalStatus.PENDING;
    private String coachNote;
    @ManyToOne
    @JoinColumn(name = "coach_user_id")
    private User coachUser;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus academicApproval = ApprovalStatus.PENDING;
    private String academicNote;
    @ManyToOne
    @JoinColumn(name = "academic_user_id")
    private User academicUser;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus accountantApproval = ApprovalStatus.PENDING;
    private String accountantNote;
    @ManyToOne
    @JoinColumn(name = "accountant_user_id")
    private User accountantUser;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus adminApproval = ApprovalStatus.PENDING;
    private String adminNote;
    @ManyToOne
    @JoinColumn(name = "admin_user_id")
    private User adminUser;

    @Enumerated(EnumType.STRING)
    private FormStatus status = FormStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ApprovalStatus {
        APPROVED, REJECTED, PENDING, PROCESSING
    }

    public enum FormStatus {
        APPROVED, REJECTED, PENDING, PROCESSING
    }

    public Long getId() {return id;    }
    public void setId(Long id) {this.id = id;    }

    public FormTemplate getTemplate() {return template;    }
    public void setTemplate(FormTemplate template) {this.template = template;    }

    public User getStudent() {return student;}
    public void setStudent(User student) {this.student = student;    }

    public String getPhoneNumber() {return phoneNumber;    }
    public void setPhoneNumber(String phoneNumber) {this.phoneNumber = phoneNumber;    }

    public String getEmail() {return email;    }
    public void setEmail(String email) {this.email = email;    }

    public Class getClassRoom() {return classRoom;    }
    public void setClassRoom(Class classRoom) {this.classRoom = classRoom;    }

    public User getCreatedBy() {return createdBy;    }
    public void setCreatedBy(User createdBy) {this.createdBy = createdBy;    }

    public String getReason() { return reason;    }
    public void setReason(String reason) {this.reason = reason;    }

    public LocalDate getStartDate() {return startDate;    }
    public void setStartDate(LocalDate startDate) {this.startDate = startDate;    }

    public LocalDate getEndDate() {return endDate;    }
    public void setEndDate(LocalDate endDate) {this.endDate = endDate;    }

    public BigDecimal getFeeAmount() {return feeAmount;    }
    public void setFeeAmount(BigDecimal feeAmount) {this.feeAmount = feeAmount;    }

    public ApprovalStatus getCoachApproval() {return coachApproval;    }
    public void setCoachApproval(ApprovalStatus coachApproval) {this.coachApproval = coachApproval;    }

    public String getCoachNote() {return coachNote;    }
    public void setCoachNote(String coachNote) { this.coachNote = coachNote;    }

    public User getCoachUser() {return coachUser;    }
    public void setCoachUser(User coachUser) {this.coachUser = coachUser;    }

    public ApprovalStatus getAcademicApproval() {return academicApproval;    }
    public void setAcademicApproval(ApprovalStatus academicApproval) {this.academicApproval = academicApproval;    }

    public String getAcademicNote() {return academicNote;    }
    public void setAcademicNote(String academicNote) {this.academicNote = academicNote;    }

    public User getAcademicUser() {return academicUser;    }
    public void setAcademicUser(User academicUser) {this.academicUser = academicUser;    }

    public ApprovalStatus getAccountantApproval() {return accountantApproval;    }
    public void setAccountantApproval(ApprovalStatus accountantApproval) {this.accountantApproval = accountantApproval;    }

    public String getAccountantNote() {return accountantNote;    }
    public void setAccountantNote(String accountantNote) {this.accountantNote = accountantNote;    }

    public User getAccountantUser() {return accountantUser;    }
    public void setAccountantUser(User accountantUser) {this.accountantUser = accountantUser;    }

    public ApprovalStatus getAdminApproval() {return adminApproval;    }
    public void setAdminApproval(ApprovalStatus adminApproval) {this.adminApproval = adminApproval;    }

    public String getAdminNote() {return adminNote;    }
    public void setAdminNote(String adminNote) {this.adminNote = adminNote;    }

    public User getAdminUser() {return adminUser;    }
    public void setAdminUser(User adminUser) {this.adminUser = adminUser;    }

    public FormStatus getStatus() {return status;    }
    public void setStatus(FormStatus status) {this.status = status;    }

    public LocalDateTime getCreatedAt() {return createdAt;    }
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;    }

    public LocalDateTime getUpdatedAt() {return updatedAt;    }
    public void setUpdatedAt(LocalDateTime updatedAt) {this.updatedAt = updatedAt;    }
}
