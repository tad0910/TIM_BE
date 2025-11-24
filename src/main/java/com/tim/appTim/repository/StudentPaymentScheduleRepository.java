package com.tim.appTim.repository;

import com.tim.appTim.entity.StudentPaymentSchedule;
import com.tim.appTim.entity.StudentPaymentSchedule.PaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentPaymentScheduleRepository extends JpaRepository<StudentPaymentSchedule, Long> {

    List<StudentPaymentSchedule> findByStudentTuitionIdAndStatus(Long studentTuitionId, PaymentStatus status);
}