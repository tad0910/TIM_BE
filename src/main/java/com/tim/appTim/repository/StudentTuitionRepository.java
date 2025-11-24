package com.tim.appTim.repository;
import com.tim.appTim.entity.StudentTuition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentTuitionRepository extends JpaRepository<StudentTuition, Long> {
}