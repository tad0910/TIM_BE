package com.tim.appTim.repository;

import com.tim.appTim.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {
    Page<Module> findAll(Pageable pageable);
}
