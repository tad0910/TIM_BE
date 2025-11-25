package com.tim.appTim.repository;

import com.tim.appTim.entity.TuitionRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TuitionRouteRepository extends JpaRepository<TuitionRoute, Long> {
    boolean existsByProgram_Id(Integer programId);
}