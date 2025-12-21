package com.tim.appTim.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tim.appTim.entity.Class;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {
    Optional<Class> findById(Long id);
    Page<Class> findAll(Pageable pageable);
    boolean existsByProgramId(Integer programId);
    List<Class> findByProgramId(Integer programId);
}