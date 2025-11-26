package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "tuition_installment_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TuitionInstallmentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tuition_route_id", nullable = false)
    private TuitionRoute tuitionRoute;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "base_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "days_from_previous", nullable = false)
    private Integer daysFromPrevious;
}
