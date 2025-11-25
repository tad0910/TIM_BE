package com.tim.appTim.service;

import com.tim.appTim.dto.PaymentRequestDTO;
import com.tim.appTim.dto.TuitionOverviewDTO;
import com.tim.appTim.dto.TuitionTransactionDTO;
import com.tim.appTim.entity.StudentPaymentSchedule;
import com.tim.appTim.entity.TuitionReceipt;
import com.tim.appTim.entity.TuitionTransaction;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.StudentPaymentScheduleRepository;
import com.tim.appTim.repository.TuitionTransactionRepository;
import com.tim.appTim.repository.TuitionReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class TuitionTransactionService {

    @Autowired
    private TuitionTransactionRepository transactionRepository;

    @Autowired
    private StudentPaymentScheduleRepository scheduleRepository;

    @Autowired
    private TuitionReceiptRepository receiptRepository;

    public TuitionOverviewDTO getStudentOverview(Long studentId) {
        TuitionOverviewDTO overview = transactionRepository.getOverviewByStudentId(studentId);

        if (overview == null || overview.getTotalPaid() == null) {
            return TuitionOverviewDTO.builder()
                    .totalPaid(BigDecimal.ZERO)
                    .totalRefunded(BigDecimal.ZERO)
                    .totalException(BigDecimal.ZERO)
                    .totalUsed(BigDecimal.ZERO)
                    .currentBalance(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal balance = overview.getTotalPaid()
                .subtract(overview.getTotalRefunded())
                .subtract(overview.getTotalUsed())
                .add(overview.getTotalException());

        overview.setCurrentBalance(balance);
        return overview;
    }

    public TuitionOverviewDTO getAdminOverview() {
        return transactionRepository.getSystemOverview();
    }

    @Transactional
    public TuitionReceipt processPayment(PaymentRequestDTO request, User collector) {

        StudentPaymentSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Đợt đóng tiền không tồn tại"));

        if (schedule.getStatus() == StudentPaymentSchedule.PaymentStatus.PAID) {
            throw new BadRequestException("Đợt này đã hoàn thành đóng tiền rồi!");
        }

        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);
        schedule.setPaidAmount(schedule.getExpectedAmount());
        scheduleRepository.save(schedule);

        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setStudentTuition(schedule.getStudentTuition());
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT);
        transaction.setAmount(schedule.getExpectedAmount());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPerformedBy(collector);
        transaction.setDescription("Thanh toán đợt " + schedule.getInstallmentNumber() + " - " + request.getPaymentMethod());

        transactionRepository.save(transaction);

        TuitionReceipt receipt = new TuitionReceipt();
        receipt.setPaymentSchedule(schedule);
        receipt.setAmount(schedule.getExpectedAmount());
        receipt.setPaymentDate(LocalDateTime.now());
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setNote(request.getNote());
        receipt.setCollector(collector);

        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        receipt.setReceiptCode("REC-" + datePart + "-" + uniquePart);

        return receiptRepository.save(receipt);
    }

    public Page<TuitionTransactionDTO> getTransactionHistory(Long studentId, Pageable pageable) {
        Page<TuitionTransaction> transactions = transactionRepository
                .findByStudentTuition_Student_IdOrderByTransactionDateDesc(studentId, pageable);

        return transactions.map(TuitionTransactionDTO::new);
    }

    public Page<TuitionTransactionDTO> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(TuitionTransactionDTO::new);
    }
}
