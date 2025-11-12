package com.tim.appTim.repository;

import com.tim.appTim.entity.ModuleSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ModuleSessionRepository extends JpaRepository<ModuleSession, Long> {
    List<ModuleSession> findByModuleId(Integer moduleId);
    List<ModuleSession> findByModuleIdOrderBySessionNumberAsc(Integer moduleId);
    Page<ModuleSession> findByModuleId(Integer moduleId, Pageable pageable);
    Page<ModuleSession> findByModuleIdOrderBySessionNumberAsc(Integer moduleId, Pageable pageable);
}
