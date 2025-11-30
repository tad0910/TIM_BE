package com.tim.appTim.repository;

import com.tim.appTim.entity.TuitionReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface TuitionReceiptRepository extends JpaRepository<TuitionReceipt, Long> {

    Optional<TuitionReceipt> findByReceiptCode(String receiptCode);

    Optional<TuitionReceipt> findTopByPaymentSchedule_StudentTuition_IdAndAmountOrderByPaymentDateDesc(
            Long studentTuitionId,
            BigDecimal amount);

    Optional<TuitionReceipt> findByTransaction_Id(Long transactionId);
}