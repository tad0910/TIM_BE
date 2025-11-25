package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tuition_receipts")
@Data
public class TuitionReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String receiptCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_schedule_id", nullable = false)
    private StudentPaymentSchedule paymentSchedule;

    private BigDecimal amount;
    private LocalDateTime paymentDate;

    private String paymentMethod;
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id")
    private User collector;
}