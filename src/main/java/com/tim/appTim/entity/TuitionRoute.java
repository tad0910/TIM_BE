package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tuition_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TuitionRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TuitionRouteType type;

    @Column(name = "admission_fee")
    private BigDecimal admissionFee;

    @Column(name = "first_month_fee")
    private BigDecimal firstMonthFee;

    @Column(name = "total_listed_fee")
    private BigDecimal totalListedFee;

    @Column(name = "number_of_installments")
    private Integer numberOfInstallments;

    @Column(name = "frequency")
    private Integer frequency;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "tuitionRoute", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<TuitionInstallmentConfig> installmentConfigs = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Programs program;

    public enum TuitionRouteType {
        FULL_TIME,
        PART_TIME
    }
}