package com.tim.appTim.repository;

import com.tim.appTim.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByClassEntityId(Long classId);

    List<JobApplication> findByStudentId(Long studentId);
}