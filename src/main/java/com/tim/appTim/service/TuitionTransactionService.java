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
import java.util.Optional;
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
        java.util.List<StudentPaymentSchedule> schedules = scheduleRepository
                .findByStudentTuition_Student_Id(studentId);

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
            BigDecimal listedSum = row != null && row.length > 0 && row[0] != null ? (BigDecimal) row[0]
                    : BigDecimal.ZERO;
            BigDecimal admissionSum = row != null && row.length > 1 && row[1] != null ? (BigDecimal) row[1]
                    : BigDecimal.ZERO;
            listedPlusAdmissionSum = listedSum.add(admissionSum);
        }

        BigDecimal totalWaived = listedPlusAdmissionSum.subtract(totalExpected);
        if (totalWaived.compareTo(BigDecimal.ZERO) < 0) {
            totalWaived = BigDecimal.ZERO;
        }

        BigDecimal remaining = totalExpected.subtract(totalPaid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0)
            remaining = BigDecimal.ZERO;

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
        if (request.getStudentId() == null || request.getAmount() == null) {
            throw new BadRequestException("Thông tin thanh toán không hợp lệ (thiếu studentId hoặc amount)");
        }

        BigDecimal remainingPayment = request.getAmount();
        if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền đóng phải lớn hơn 0");
        }

        // 1. Get all schedules for the student, sorted by installment number
        List<StudentPaymentSchedule> schedules = scheduleRepository
                .findByStudentTuition_Student_Id(request.getStudentId());
        if (schedules == null || schedules.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy lộ trình học phí cho học viên này");
        }

        // Sort by installment number
        schedules.sort((a, b) -> Integer.compare(a.getInstallmentNumber(), b.getInstallmentNumber()));

        StudentTuition studentTuition = schedules.get(0).getStudentTuition();

        // 2. Distribute money (Water Pouring)
        for (StudentPaymentSchedule schedule : schedules) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            if (schedule.getStatus() == StudentPaymentSchedule.PaymentStatus.PAID) {
                continue;
            }

            BigDecimal expected = schedule.getExpectedAmount();
            BigDecimal alreadyPaid = schedule.getPaidAmount() != null ? schedule.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal needed = expected.subtract(alreadyPaid);

            if (needed.compareTo(BigDecimal.ZERO) <= 0) {
                // Should be PAID, but just in case
                schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);
                scheduleRepository.save(schedule);
                continue;
            }

            if (remainingPayment.compareTo(needed) >= 0) {
                // Pay off this installment completely
                schedule.setPaidAmount(expected);
                schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);
                remainingPayment = remainingPayment.subtract(needed);
            } else {
                // Partial payment
                schedule.setPaidAmount(alreadyPaid.add(remainingPayment));
                schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PARTIAL);
                remainingPayment = BigDecimal.ZERO;
            }
            scheduleRepository.save(schedule);
        }

        // 3. Create Transaction
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setStudentTuition(studentTuition);
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPerformedBy(collector);
        transaction.setDescription(
                request.getNote() != null ? request.getNote() : ("Thanh toán học phí - " + request.getPaymentMethod()));

        transactionRepository.save(transaction);

        // 4. Create Receipt
        TuitionReceipt receipt = new TuitionReceipt();
        // receipt.setPaymentSchedule(null); // No specific schedule anymore
        receipt.setTransaction(transaction); // Link to transaction
        receipt.setAmount(request.getAmount());
        receipt.setPaymentDate(LocalDateTime.now());
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setNote(request.getNote());
        receipt.setCollector(collector);

        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        receipt.setReceiptCode("REC-" + datePart + "-" + uniquePart);

        return receiptRepository.save(receipt);
    }

    @Transactional(readOnly = true)
    public Page<TuitionTransactionDTO> getTransactionHistory(Long studentId, Pageable pageable) {
        Page<TuitionTransaction> transactions = transactionRepository
                .findByStudentTuition_Student_IdOrderByTransactionDateDesc(studentId, pageable);

        return transactions.map(tx -> {
            TuitionTransactionDTO dto = new TuitionTransactionDTO(tx);
            if (tx.getType() == TuitionTransaction.TransactionType.PAYMENT) {
                if (tx.getId() != null) {
                    Optional<TuitionReceipt> r = receiptRepository.findByTransaction_Id(tx.getId());
                    r.ifPresent(rec -> {
                        dto.setReceiptId(rec.getId());
                        dto.setReceiptCode(rec.getReceiptCode());
                    });
                }
            }
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public Page<TuitionTransactionDTO> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllByOrderByTransactionDateDesc(pageable)
                .map(tx -> {
                    TuitionTransactionDTO dto = new TuitionTransactionDTO(tx);
                    if (tx.getType() == TuitionTransaction.TransactionType.PAYMENT) {
                        if (tx.getId() != null) {
                            Optional<TuitionReceipt> r = receiptRepository.findByTransaction_Id(tx.getId());
                            r.ifPresent(rec -> {
                                dto.setReceiptId(rec.getId());
                                dto.setReceiptCode(rec.getReceiptCode());
                            });
                        }
                    }
                    return dto;
                });
    }

    public TuitionTransaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch không tồn tại"));
    }
}
