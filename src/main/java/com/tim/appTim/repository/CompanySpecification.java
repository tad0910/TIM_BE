package com.tim.appTim.repository;

import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.Company.CompanyType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CompanySpecification {

    public static Specification<Company> filter(String keyword, String type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), likePattern));
            }

            if (type != null && !type.trim().isEmpty()) {
                try {
                    predicates.add(cb.equal(root.get("type"), CompanyType.valueOf(type)));
                } catch (IllegalArgumentException e) {
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
