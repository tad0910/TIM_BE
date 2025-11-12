package com.tim.appTim.repository;

import com.tim.appTim.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    Page<Module> findAll(Pageable pageable);

    @Query("SELECT m FROM Module m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Module> searchByName(@Param("keyword") String keyword, Pageable pageable);
}
