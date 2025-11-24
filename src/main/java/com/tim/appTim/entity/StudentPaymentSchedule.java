package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "student_payment_schedules")
@Data
public class StudentPaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_tuition_id", nullable = false)
    private StudentTuition studentTuition;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "expected_amount")
    private BigDecimal expectedAmount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @OneToOne(mappedBy = "paymentSchedule", cascade = CascadeType.ALL)
    private TuitionReceipt receipt;

    public enum PaymentStatus {
        PENDING,
        PARTIAL,
        PAID,
        OVERDUE,
        CANCELLED
    }
}

