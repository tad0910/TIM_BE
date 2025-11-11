package com.tim.appTim.repository;

import com.tim.appTim.entity.Programs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProgramsRepository extends JpaRepository<Programs, Integer> {
    Page<Programs> findAll(Pageable pageable);
    
}
