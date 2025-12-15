package com.tim.appTim.repository;

import com.tim.appTim.entity.GradeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeHistoryRepository extends JpaRepository<GradeHistory, Long> {
    List<GradeHistory> findByGradeIdOrderByChangedAtDesc(Long gradeId);

    Page<GradeHistory> findByGradeId(Long gradeId, Pageable pageable);
}