package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "student_tuitions")
@Data
public class StudentTuition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuition_route_id", nullable = false)
    private TuitionRoute tuitionRoute;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Enumerated(EnumType.STRING)
    private TuitionStatus status;

    @Column(name = "total_actual_fee")
    private BigDecimal totalActualFee;

    @OneToMany(mappedBy = "studentTuition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudentPaymentSchedule> paymentSchedules;

    public enum TuitionStatus {
        PENDING,
        ACTIVE,
        COMPLETED,
        DROPPED,
        RESERVED
    }
}

