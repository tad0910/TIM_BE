package com.tim.appTim.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_activities")
@Getter
@Setter
public class JobActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_lead_id")
    @JsonIgnoreProperties({"student", "hibernateLazyInitializer", "handler"})
    private JobLead jobLead;

    @Column(name = "job_lead_id", insertable = false, updatable = false)
    private Long jobLeadId;

    @Enumerated(EnumType.STRING)
    private JobActivityType activityType;

    private String fileUrl;

    private String salaryAmount;

    private LocalDate happenedAt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt;
}