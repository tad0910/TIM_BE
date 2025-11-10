package com.tim.appTim.repository;

import com.tim.appTim.entity.ClassModuleTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassModuleTeacherRepository extends JpaRepository<ClassModuleTeacher, Long> {
    List<ClassModuleTeacher> findByClassModuleId(Long classModuleId);
    
    List<ClassModuleTeacher> findByUserId(Long userId);
    
    Optional<ClassModuleTeacher> findByClassModuleIdAndUserId(Long classModuleId, Long userId);
    
    boolean existsByClassModuleIdAndUserId(Long classModuleId, Long userId);
    
    void deleteByClassModuleId(Long classModuleId);
}

