package com.tim.appTim.repository;

import com.tim.appTim.entity.ClassModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassModuleRepository extends JpaRepository<ClassModule, Long> {
    List<ClassModule> findByClassId(Long classId);
    
    List<ClassModule> findByModuleId(Integer moduleId);
    
    Optional<ClassModule> findByClassIdAndModuleId(Long classId, Integer moduleId);
    
    boolean existsByClassIdAndModuleId(Long classId, Integer moduleId);
    
    @Query("SELECT cm FROM ClassModule cm WHERE cm.classId = :classId ORDER BY cm.createdAt ASC")
    List<ClassModule> findByClassIdOrderByCreatedAt(@Param("classId") Long classId);
}

