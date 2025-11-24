package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "coupons")
@Data
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String centerScope = "TOAN_HE_THONG";

    @Column(nullable = false)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Enumerated(EnumType.STRING)
    private CouponScenario scenario;

    private boolean active = true;

    private Integer quantity;
    private Integer usedCount = 0;

    public enum DiscountType {
        AMOUNT,
        PERCENT
    }

    public enum CouponScenario {
        SPREAD_EVENLY,
        DEDUCT_FIRST_FULL,
        DEDUCT_LAST_FULL,
        PARTIAL_FIRST_THEN_SPREAD
    }
}