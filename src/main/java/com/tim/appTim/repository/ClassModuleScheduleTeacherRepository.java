package com.tim.appTim.repository;

import com.tim.appTim.entity.ClassModuleScheduleTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassModuleScheduleTeacherRepository extends JpaRepository<ClassModuleScheduleTeacher, Long> {
    List<ClassModuleScheduleTeacher> findByClassModuleScheduleId(Long classModuleScheduleId);
    
    List<ClassModuleScheduleTeacher> findByUserId(Long userId);
    
    Optional<ClassModuleScheduleTeacher> findByClassModuleScheduleIdAndUserId(Long classModuleScheduleId, Long userId);
    
    boolean existsByClassModuleScheduleIdAndUserId(Long classModuleScheduleId, Long userId);
    
    void deleteByClassModuleScheduleId(Long classModuleScheduleId);
}

