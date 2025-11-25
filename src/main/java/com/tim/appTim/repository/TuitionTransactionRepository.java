package com.tim.appTim.repository;

import com.tim.appTim.dto.TuitionOverviewDTO;
import com.tim.appTim.entity.TuitionTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TuitionTransactionRepository extends JpaRepository<TuitionTransaction, Long> {

    @Query("SELECT new com.tim.appTim.dto.TuitionOverviewDTO(" +
            "SUM(CASE WHEN t.type = 'PAYMENT' THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN t.type = 'REFUND' THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN t.type = 'EXCEPTION' THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN t.type = 'USAGE' THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN t.type = 'PAYMENT' THEN t.amount ELSE 0 END) " +
            ") " +
            "FROM TuitionTransaction t " +
            "WHERE t.studentTuition.student.id = :studentId")
    TuitionOverviewDTO getOverviewByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT new com.tim.appTim.dto.TuitionOverviewDTO(" +
            "SUM(CASE WHEN t.type = 'PAYMENT' THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN t.type = 'REFUND' THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN t.type = 'EXCEPTION' THEN t.amount ELSE 0 END), " +
            "SUM(CASE WHEN t.type = 'USAGE' THEN t.amount ELSE 0 END), " +
            "CAST(0 AS bigdecimal) ) " +
            "FROM TuitionTransaction t")
    TuitionOverviewDTO getSystemOverview();

    Page<TuitionTransaction> findByStudentTuition_Student_IdOrderByTransactionDateDesc(
            Long studentId, Pageable pageable);
}