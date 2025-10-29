package com.tim.appTim.repository;

import com.tim.appTim.entity.ProgramModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProgramModuleRepository extends JpaRepository<ProgramModule, ProgramModule.ProgramModuleId> {
    List<ProgramModule> findByProgramId(Integer programId);
    List<ProgramModule> findByModuleId(Integer moduleId);
    List<ProgramModule> findByProgramIdOrderByPositionAsc(Integer programId);
    
    // Query tối ưu với JOIN FETCH để load module data cùng lúc, tránh N+1 problem
    @Query("SELECT pm FROM ProgramModule pm " +
           "LEFT JOIN FETCH pm.module " +
           "WHERE pm.program.id = :programId " +
           "ORDER BY pm.position ASC")
    List<ProgramModule> findByProgramIdWithModule(@Param("programId") Integer programId);
}
