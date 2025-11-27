package com.tim.appTim.service;

import com.tim.appTim.dto.PaymentRequestDTO;
import com.tim.appTim.dto.TuitionOverviewDTO;
import com.tim.appTim.dto.TuitionTransactionDTO;
import com.tim.appTim.dto.StudentPaymentScheduleDTO;
import com.tim.appTim.entity.StudentPaymentSchedule;
import com.tim.appTim.entity.StudentTuition;
import com.tim.appTim.entity.TuitionReceipt;
import com.tim.appTim.entity.TuitionTransaction;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.StudentPaymentScheduleRepository;
import com.tim.appTim.repository.TuitionTransactionRepository;
import com.tim.appTim.repository.TuitionReceiptRepository;
import com.tim.appTim.repository.StudentTuitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TuitionTransactionService {

    @Autowired
    private TuitionTransactionRepository transactionRepository;

    @Autowired
    private StudentPaymentScheduleRepository scheduleRepository;

    @Autowired
    private TuitionReceiptRepository receiptRepository;

    @Autowired
    private StudentTuitionRepository studentTuitionRepository;

    @Transactional(readOnly = true)
    public TuitionOverviewDTO getStudentOverview(Long studentId) {
        java.util.List<StudentPaymentSchedule> schedules = scheduleRepository.findByStudentTuition_Student_Id(studentId);
        
        BigDecimal totalExpected = BigDecimal.ZERO; 
        BigDecimal totalPaid = BigDecimal.ZERO;     

        if (schedules != null && !schedules.isEmpty()) {
            for (StudentPaymentSchedule sch : schedules) {
                BigDecimal expected = sch.getExpectedAmount() != null ? sch.getExpectedAmount() : BigDecimal.ZERO;
                totalExpected = totalExpected.add(expected);

                switch (sch.getStatus()) {
                    case PAID:
                        totalPaid = totalPaid.add(expected);
                        break;
                    case PARTIAL:
                        totalPaid = totalPaid.add(sch.getPaidAmount() != null ? sch.getPaidAmount() : BigDecimal.ZERO);
                        break;
                    default:
                        break;
                }
            }
        }

        BigDecimal listedPlusAdmissionSum = BigDecimal.ZERO;
        java.util.List<Object[]> sums = studentTuitionRepository.sumListedAndAdmissionByStudent(studentId);
        if (sums != null && !sums.isEmpty()) {
            Object[] row = sums.get(0);
            BigDecimal listedSum = row != null && row.length > 0 && row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
            BigDecimal admissionSum = row != null && row.length > 1 && row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            listedPlusAdmissionSum = listedSum.add(admissionSum);
        }
        
        BigDecimal totalWaived = listedPlusAdmissionSum.subtract(totalExpected);
        if (totalWaived.compareTo(BigDecimal.ZERO) < 0) {
            totalWaived = BigDecimal.ZERO;
        }

        BigDecimal remaining = totalExpected.subtract(totalPaid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        return TuitionOverviewDTO.builder()
                .totalPaid(totalPaid)
                .totalRefunded(BigDecimal.ZERO)
                .totalException(BigDecimal.ZERO)
                .totalUsed(totalPaid)
                .currentBalance(remaining)
                .totalWaived(totalWaived)
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.List<StudentPaymentScheduleDTO> getStudentSchedules(Long studentId) {
        List<StudentPaymentSchedule> schedules = scheduleRepository.findByStudentTuition_Student_Id(studentId);
        if (schedules == null || schedules.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return schedules.stream()
                .sorted((a, b) -> {
                    Integer ia = a.getInstallmentNumber() != null ? a.getInstallmentNumber() : Integer.MAX_VALUE;
                    Integer ib = b.getInstallmentNumber() != null ? b.getInstallmentNumber() : Integer.MAX_VALUE;
                    return Integer.compare(ia, ib);
                })
                .map(StudentPaymentScheduleDTO::new)
                .collect(Collectors.toList());
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
