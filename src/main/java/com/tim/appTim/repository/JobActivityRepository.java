package com.tim.appTim.repository;

import com.tim.appTim.entity.JobActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 
 */
@Repository
public interface JobActivityRepository extends JpaRepository<JobActivity, Long> {
    List<JobActivity> findByJobLeadIdOrderByHappenedAtDesc(Long jobLeadId);

    Optional<JobActivity> findTopByJobLeadIdOrderByCreatedAtDesc(Long jobLeadId);
}