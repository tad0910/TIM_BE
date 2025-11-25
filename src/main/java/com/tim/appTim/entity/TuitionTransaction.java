package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tuition_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TuitionTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_tuition_id", nullable = false)
    private StudentTuition studentTuition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private BigDecimal amount;

    private LocalDateTime transactionDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    public enum TransactionType {
        PAYMENT,
        REFUND,
        EXCEPTION,
        USAGE,
        SCHOLARSHIP
    }
}