package com.tim.appTim.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String shortName;

    @Enumerated(EnumType.STRING)
    private CompanyType type;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_technologies", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "tech_name")
    private Set<String> technologies = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_regions", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "region_name")
    private Set<String> regions = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_markets", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "market_name")
    private Set<String> markets = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "company_products", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "product_name")
    private Set<String> products = new HashSet<>();

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String logoUrl;
    private String website;

    private String profileUrl;

    private String phone;
    private LocalDate foundingDate;
    private String size;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ContactPerson> contacts;

    private LocalDateTime createdAt;
    private String status;

    public enum CompanyType {
        CODEGYM_TO_PARTNER("Codegym tiếp cận đối tác"),
        PARTNER_TO_CODEGYM("Đối tác tiếp cận Codegym"),

        OFFICIAL_PARTNER("Đối tác chiến lược"),
        POTENTIAL_PARTNER("Đối tác tiềm năng"),
        FORMER_PARTNER("Đối tác cũ");

        private final String displayName;

        CompanyType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}