package com.tim.appTim.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_leads")
@Getter
@Setter
public class JobLead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    private String shortName;

    private String address;

    private String website;
    @Enumerated(EnumType.STRING)
    private LeadStatus status;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private User student;

    @OneToMany(mappedBy = "jobLead", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<JobActivity> activities = new ArrayList<>();

    public enum LeadStatus {
        NEW("Mới tạo"),
        APPLIED("Đã ứng tuyển"),
        INTERVIEWING("Đang phỏng vấn"),
        OFFER("Nhận Offer"),
        FAILED("Trượt"),
        IGNORED("Bỏ qua");

        private final String displayName;
        LeadStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}