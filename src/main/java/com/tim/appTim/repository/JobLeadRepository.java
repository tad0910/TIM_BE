package com.tim.appTim.repository;

import com.tim.appTim.entity.JobLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobLeadRepository extends JpaRepository<JobLead, Long> {
    List<JobLead> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}