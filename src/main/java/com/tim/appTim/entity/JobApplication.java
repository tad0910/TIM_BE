package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
public class JobApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Class classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    private String cvUrl;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private LocalDateTime appliedAt;

    private String note;

    public enum ApplicationStatus {
        SENT_CV("Gửi CV"),
        INTERVIEW_SCHEDULED("Nhận lịch phỏng vấn"),
        INTERVIEW("Phỏng vấn"),
        OFFER_RECEIVED("Nhận offer"),
        PROBATION_CONTRACT("Ký hợp đồng thử việc"),
        OFFICIAL_CONTRACT("Ký hợp đồng chính thức"),

        FAILED("Trượt"),
        CANCELLED("Hủy");

        private final String displayName;
        ApplicationStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}