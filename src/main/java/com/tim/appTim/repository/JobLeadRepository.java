package com.tim.appTim.repository;

import com.tim.appTim.entity.JobLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing JobLead entities.
 */
@Repository
public interface JobLeadRepository extends JpaRepository<JobLead, Long> {
    /**
     * Finds all JobLeads for a given student ID, ordered by creation date in descending order.
     *
     * @param studentId the ID of the student to find JobLeads for
     * @return a list of JobLeads for the given student ID
     */
    List<JobLead> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /**
     * Finds the most recent JobLead for a given student ID, ordered by creation date in descending order.
     *
     * @param studentId the ID of the student to find the most recent JobLead for
     * @return the most recent JobLead for the given student ID, or an empty Optional if no JobLead is found
     */
    Optional<JobLead> findTopByStudentIdOrderByCreatedAtDesc(Long studentId);

    /**
     * Finds all JobLeads for a list of given student IDs.
     *
     * @param studentIds the IDs of the students to find JobLeads for
     * @return a list of JobLeads for the given student IDs
     */
    List<JobLead> findByStudentIdIn(List<Long> studentIds);

    long countByStatus(JobLead.LeadStatus status);

    List<JobLead> findTop5ByOrderByCreatedAtDesc();
}