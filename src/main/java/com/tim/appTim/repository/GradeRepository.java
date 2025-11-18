package com.tim.appTim.repository;

import com.tim.appTim.entity.Grade; // (Entity MỚI của bạn)
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByStudentIdAndClassModuleIdAndStatus(Long studentId, Long classModuleId, Grade.Status status);

    List<Grade> findByClassModuleIdAndStudentIdInAndStatus(
            Long classModuleId, List<Long> studentIds, Grade.Status status);

    Optional<Grade> findByClassModuleIdAndStudentIdAndStatus(
            Long classModuleId, Long studentId, Grade.Status status);
}