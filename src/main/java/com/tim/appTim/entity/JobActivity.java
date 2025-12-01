package com.tim.appTim.entity;

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

    // Liên kết với đầu mối việc làm (Cha)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_lead_id")
    private JobLead jobLead;

    // Loại hoạt động (Trùng với Status của JobLead)
    // VD: SENT_CV, INTERVIEW, OFFER_RECEIVED...
    @Enumerated(EnumType.STRING)
    private JobLead.LeadStatus activityType;

    // File đính kèm (Ảnh offer, Hợp đồng...)
    private String fileUrl;

    // Trường đa năng: Lưu "Mức offer" hoặc "Mức lương thử việc"
    // Lưu String để linh hoạt (VD: "10 triệu gross" hoặc "10,000,000")
    private String salaryAmount;

    // Thời gian diễn ra (Input ngày trên form)
    private LocalDate happenedAt;

    // Nội dung chính (Mô tả lúc tạo mới)
    @Column(columnDefinition = "TEXT")
    private String content;

    // [MỚI] Ghi chú bổ sung (Dùng cho tính năng update note sau này)
    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt;
}