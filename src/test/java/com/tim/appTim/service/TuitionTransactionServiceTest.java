package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.StudentPaymentSchedule;
import com.tim.appTim.entity.StudentTuition;
import com.tim.appTim.entity.TuitionReceipt;
import com.tim.appTim.entity.TuitionTransaction;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.StudentPaymentScheduleRepository;
import com.tim.appTim.repository.StudentTuitionRepository;
import com.tim.appTim.repository.TuitionReceiptRepository;
import com.tim.appTim.repository.TuitionTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TuitionTransactionServiceTest {

    @Mock
    private TuitionTransactionRepository transactionRepository;
    @Mock
    private StudentPaymentScheduleRepository scheduleRepository;
    @Mock
    private TuitionReceiptRepository receiptRepository;
    @Mock
    private StudentTuitionRepository studentTuitionRepository;

    @InjectMocks
    private TuitionTransactionService transactionService;

    private User student;
    private User collector;
    private StudentTuition studentTuition;
    private StudentPaymentSchedule schedule1;
    private StudentPaymentSchedule schedule2;
    private List<StudentPaymentSchedule> schedules;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setUsername("student1");

        collector = new User();
        collector.setId(2L);
        collector.setUsername("admin");

        studentTuition = new StudentTuition();
        studentTuition.setId(1L);
        studentTuition.setStudent(student);

        schedule1 = new StudentPaymentSchedule();
        schedule1.setId(101L);
        schedule1.setStudentTuition(studentTuition);
        schedule1.setInstallmentNumber(1);
        schedule1.setExpectedAmount(new BigDecimal("1000000")); // 1,000,000
        schedule1.setPaidAmount(BigDecimal.ZERO);
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);

        schedule2 = new StudentPaymentSchedule();
        schedule2.setId(102L);
        schedule2.setStudentTuition(studentTuition);
        schedule2.setInstallmentNumber(2);
        schedule2.setExpectedAmount(new BigDecimal("2000000")); // 2,000,000
        schedule2.setPaidAmount(BigDecimal.ZERO);
        schedule2.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);

        schedules = new ArrayList<>();
        schedules.add(schedule1);
        schedules.add(schedule2);
    }

    @Test
    void processPayment_FullPayment_ShouldMarkAllPaid() {
        // Arrange
        BigDecimal paymentAmount = new BigDecimal("3000000"); // 1M + 2M
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(paymentAmount);
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        verify(scheduleRepository, times(2)).save(any(StudentPaymentSchedule.class));

        assertThat(schedule1.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);
        assertThat(schedule1.getPaidAmount()).isEqualByComparingTo("1000000");

        assertThat(schedule2.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);
        assertThat(schedule2.getPaidAmount()).isEqualByComparingTo("2000000");
    }

    @Test
    void processPayment_PartialPayment_WaterPouring() {
        // Arrange
        BigDecimal paymentAmount = new BigDecimal("1500000"); // Pays 1M (full) + 0.5M (partial)
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(paymentAmount);
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        verify(scheduleRepository, atLeastOnce()).save(any(StudentPaymentSchedule.class));

        // Schedule 1: 1M expected, fully paid
        assertThat(schedule1.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);
        assertThat(schedule1.getPaidAmount()).isEqualByComparingTo("1000000");

        // Schedule 2: 2M expected, 0.5M paid -> Partial
        assertThat(schedule2.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PARTIAL);
        assertThat(schedule2.getPaidAmount()).isEqualByComparingTo("500000");
    }

    @Test
    void processPayment_ExcessPayment_ShouldPayAllAndRemain() {
        // Arrange
        BigDecimal paymentAmount = new BigDecimal("5000000"); // 3M needed, 2M excess
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(paymentAmount);
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        assertThat(schedule1.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);
        assertThat(schedule2.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);

        // Verify transaction amount records the full payment
        ArgumentCaptor<TuitionTransaction> txCaptor = ArgumentCaptor.forClass(TuitionTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("5000000");
    }

    @Test
    void processPayment_ZeroOrNegativeAmount_ShouldThrowException() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> transactionService.processPayment(request, collector))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số tiền đóng phải lớn hơn 0");

        request.setAmount(new BigDecimal("-100"));
        assertThatThrownBy(() -> transactionService.processPayment(request, collector))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Số tiền đóng phải lớn hơn 0");
    }

    @Test
    void processPayment_NoDebts_ShouldThrowException() {
        // Arrange
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(new BigDecimal("1000000"));

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.processPayment(request, collector))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lộ trình học phí");
    }

    @Test
    void processPayment_SkipPaidSchedules() {
        // Arrange
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);
        schedule1.setPaidAmount(schedule1.getExpectedAmount());

        BigDecimal paymentAmount = new BigDecimal("500000"); // Pay 0.5M for schedule 2
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(paymentAmount);
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        // Schedule 1 should remain PAID and untouched (logic skips it)
        assertThat(schedule1.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);

        // Schedule 2 should receive the payment
        assertThat(schedule2.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PARTIAL);
        assertThat(schedule2.getPaidAmount()).isEqualByComparingTo("500000");
    }

    @Test
    void processPayment_TopUp_ExistingPartial_ShouldAccumulate() {
        // Arrange
        // Schedule 1 is already partially paid (500k paid out of 1M)
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PARTIAL);
        schedule1.setPaidAmount(new BigDecimal("500000"));

        BigDecimal paymentAmount = new BigDecimal("300000"); // Pay another 300k
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(paymentAmount);
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        // Schedule 1 should remain PARTIAL but paid amount should increase to 800k
        assertThat(schedule1.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PARTIAL);
        assertThat(schedule1.getPaidAmount()).isEqualByComparingTo("800000"); // 500k + 300k

        // Schedule 2 should remain untouched
        assertThat(schedule2.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PENDING);
        assertThat(schedule2.getPaidAmount()).isEqualByComparingTo("0");
    }

    // --- getStudentOverview tests ---

    @Test
    void getStudentOverview_WithSchedules() {
        // Arrange
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);
        schedule1.setPaidAmount(new BigDecimal("1000000"));
        schedule2.setStatus(StudentPaymentSchedule.PaymentStatus.PARTIAL);
        schedule2.setPaidAmount(new BigDecimal("500000"));

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.emptyList());

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalPaid()).isEqualByComparingTo("1500000"); // 1M + 0.5M
        assertThat(result.getCurrentBalance()).isEqualByComparingTo("1500000"); // 3M - 1.5M
    }

    @Test
    void getStudentOverview_EmptySchedules() {
        // Arrange
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.emptyList());
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.emptyList());

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalPaid()).isEqualByComparingTo("0");
        assertThat(result.getCurrentBalance()).isEqualByComparingTo("0");
    }

    @Test
    void getStudentOverview_WithNullExpectedAmount() {
        // Arrange
        schedule1.setExpectedAmount(null);
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.emptyList());

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalPaid()).isEqualByComparingTo("0");
    }

    @Test
    void getStudentOverview_WithNullPaidAmount() {
        // Arrange
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PARTIAL);
        schedule1.setPaidAmount(null);
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.emptyList());

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalPaid()).isEqualByComparingTo("0");
    }

    @Test
    void getStudentOverview_WithWaivedAmount() {
        // Arrange
        schedule1.setExpectedAmount(new BigDecimal("1000000"));
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        
        // Listed + Admission = 2M, Expected = 1M, Waived = 1M
        Object[] sumRow = new Object[]{new BigDecimal("1500000"), new BigDecimal("500000")};
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.singletonList(sumRow));

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalWaived()).isEqualByComparingTo("1000000"); // 2M - 1M
    }

    @Test
    void getStudentOverview_NegativeWaived() {
        // Arrange
        schedule1.setExpectedAmount(new BigDecimal("3000000")); // More than listed+admission
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        
        Object[] sumRow = new Object[]{new BigDecimal("1500000"), new BigDecimal("500000")};
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.singletonList(sumRow));

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalWaived()).isEqualByComparingTo("0"); // Should be 0, not negative
    }

    @Test
    void getStudentOverview_NegativeRemaining() {
        // Arrange
        schedule1.setExpectedAmount(new BigDecimal("1000000"));
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);
        schedule1.setPaidAmount(new BigDecimal("2000000")); // Paid more than expected
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.emptyList());

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCurrentBalance()).isEqualByComparingTo("0"); // Should be 0, not negative
    }

    @Test
    void getStudentOverview_WithNullSums() {
        // Arrange
        schedule1.setExpectedAmount(new BigDecimal("1000000"));
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(null);

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalWaived()).isEqualByComparingTo("0");
    }

    @Test
    void getStudentOverview_WithEmptySums() {
        // Arrange
        schedule1.setExpectedAmount(new BigDecimal("1000000"));
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.emptyList());

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalWaived()).isEqualByComparingTo("0");
    }

    @Test
    void getStudentOverview_WithNullSumRow() {
        // Arrange
        schedule1.setExpectedAmount(new BigDecimal("1000000"));
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.singletonList(null));

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalWaived()).isEqualByComparingTo("0");
    }

    @Test
    void getStudentOverview_WithNullSumValues() {
        // Arrange
        schedule1.setExpectedAmount(new BigDecimal("1000000"));
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        Object[] sumRow = new Object[]{null, null};
        when(studentTuitionRepository.sumListedAndAdmissionByStudent(1L)).thenReturn(Collections.singletonList(sumRow));

        // Act
        TuitionOverviewDTO result = transactionService.getStudentOverview(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalWaived()).isEqualByComparingTo("0");
    }

    // --- getStudentSchedules tests ---

    @Test
    void getStudentSchedules_WithData() {
        // Arrange
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);

        // Act
        java.util.List<StudentPaymentScheduleDTO> result = transactionService.getStudentSchedules(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(result.get(1).getInstallmentNumber()).isEqualTo(2);
    }

    @Test
    void getStudentSchedules_Empty() {
        // Arrange
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.emptyList());

        // Act
        java.util.List<StudentPaymentScheduleDTO> result = transactionService.getStudentSchedules(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getStudentSchedules_Null() {
        // Arrange
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(null);

        // Act
        java.util.List<StudentPaymentScheduleDTO> result = transactionService.getStudentSchedules(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getStudentSchedules_SortedByInstallmentNumber() {
        // Arrange
        StudentPaymentSchedule schedule3 = new StudentPaymentSchedule();
        schedule3.setInstallmentNumber(3);
        schedule3.setExpectedAmount(new BigDecimal("3000000"));
        schedules.add(schedule3);
        
        // Reverse order
        Collections.reverse(schedules);
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(schedules);

        // Act
        java.util.List<StudentPaymentScheduleDTO> result = transactionService.getStudentSchedules(1L);

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(result.get(1).getInstallmentNumber()).isEqualTo(2);
        assertThat(result.get(2).getInstallmentNumber()).isEqualTo(3);
    }

    @Test
    void getStudentSchedules_WithNullInstallmentNumber() {
        // Arrange
        schedule1.setInstallmentNumber(null);
        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));

        // Act
        java.util.List<StudentPaymentScheduleDTO> result = transactionService.getStudentSchedules(1L);

        // Assert
        assertThat(result).hasSize(1);
    }

    // --- getAdminOverview tests ---

    @Test
    void getAdminOverview_Success() {
        // Arrange
        TuitionOverviewDTO overview = TuitionOverviewDTO.builder()
                .totalPaid(new BigDecimal("10000000"))
                .totalRefunded(new BigDecimal("500000"))
                .totalException(new BigDecimal("200000"))
                .totalUsed(new BigDecimal("9500000"))
                .currentBalance(new BigDecimal("500000"))
                .totalWaived(new BigDecimal("1000000"))
                .build();
        when(transactionRepository.getSystemOverview()).thenReturn(overview);

        // Act
        TuitionOverviewDTO result = transactionService.getAdminOverview();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalPaid()).isEqualByComparingTo("10000000");
    }

    // --- getTransactionHistory tests ---

    @Test
    void getTransactionHistory_WithReceipt() {
        // Arrange
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setId(1L);
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT);
        transaction.setAmount(new BigDecimal("1000000"));
        transaction.setStudentTuition(studentTuition);

        TuitionReceipt receipt = new TuitionReceipt();
        receipt.setId(10L);
        receipt.setReceiptCode("REC-20240101-ABC123");
        receipt.setTransaction(transaction);

        org.springframework.data.domain.Page<TuitionTransaction> page = new org.springframework.data.domain.PageImpl<>(
                Collections.singletonList(transaction));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(transactionRepository.findByStudentTuition_Student_IdOrderByTransactionDateDesc(1L, pageable))
                .thenReturn(page);
        when(receiptRepository.findByTransaction_Id(1L)).thenReturn(Optional.of(receipt));

        // Act
        org.springframework.data.domain.Page<TuitionTransactionDTO> result = 
                transactionService.getTransactionHistory(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReceiptId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getReceiptCode()).isEqualTo("REC-20240101-ABC123");
    }

    @Test
    void getTransactionHistory_WithoutReceipt() {
        // Arrange
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setId(1L);
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT);
        transaction.setAmount(new BigDecimal("1000000"));
        transaction.setStudentTuition(studentTuition);

        org.springframework.data.domain.Page<TuitionTransaction> page = new org.springframework.data.domain.PageImpl<>(
                Collections.singletonList(transaction));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(transactionRepository.findByStudentTuition_Student_IdOrderByTransactionDateDesc(1L, pageable))
                .thenReturn(page);
        when(receiptRepository.findByTransaction_Id(1L)).thenReturn(Optional.empty());

        // Act
        org.springframework.data.domain.Page<TuitionTransactionDTO> result = 
                transactionService.getTransactionHistory(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReceiptId()).isNull();
    }

    @Test
    void getTransactionHistory_NonPaymentType() {
        // Arrange
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setId(1L);
        transaction.setType(TuitionTransaction.TransactionType.REFUND);
        transaction.setAmount(new BigDecimal("1000000"));
        transaction.setStudentTuition(studentTuition);

        org.springframework.data.domain.Page<TuitionTransaction> page = new org.springframework.data.domain.PageImpl<>(
                Collections.singletonList(transaction));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(transactionRepository.findByStudentTuition_Student_IdOrderByTransactionDateDesc(1L, pageable))
                .thenReturn(page);

        // Act
        org.springframework.data.domain.Page<TuitionTransactionDTO> result = 
                transactionService.getTransactionHistory(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(receiptRepository, never()).findByTransaction_Id(anyLong());
    }

    @Test
    void getTransactionHistory_NullTransactionId() {
        // Arrange
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setId(null);
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT);
        transaction.setStudentTuition(studentTuition);

        org.springframework.data.domain.Page<TuitionTransaction> page = new org.springframework.data.domain.PageImpl<>(
                Collections.singletonList(transaction));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(transactionRepository.findByStudentTuition_Student_IdOrderByTransactionDateDesc(1L, pageable))
                .thenReturn(page);

        // Act
        org.springframework.data.domain.Page<TuitionTransactionDTO> result = 
                transactionService.getTransactionHistory(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        verify(receiptRepository, never()).findByTransaction_Id(anyLong());
    }

    // --- getAllTransactions tests ---

    @Test
    void getAllTransactions_WithReceipt() {
        // Arrange
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setId(1L);
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT);
        transaction.setAmount(new BigDecimal("1000000"));
        transaction.setStudentTuition(studentTuition);

        TuitionReceipt receipt = new TuitionReceipt();
        receipt.setId(10L);
        receipt.setReceiptCode("REC-20240101-ABC123");

        org.springframework.data.domain.Page<TuitionTransaction> page = new org.springframework.data.domain.PageImpl<>(
                Collections.singletonList(transaction));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(transactionRepository.findAllByOrderByTransactionDateDesc(pageable)).thenReturn(page);
        when(receiptRepository.findByTransaction_Id(1L)).thenReturn(Optional.of(receipt));

        // Act
        org.springframework.data.domain.Page<TuitionTransactionDTO> result = 
                transactionService.getAllTransactions(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReceiptId()).isEqualTo(10L);
    }

    @Test
    void getAllTransactions_WithoutReceipt() {
        // Arrange
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setId(1L);
        transaction.setType(TuitionTransaction.TransactionType.PAYMENT);

        org.springframework.data.domain.Page<TuitionTransaction> page = new org.springframework.data.domain.PageImpl<>(
                Collections.singletonList(transaction));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(transactionRepository.findAllByOrderByTransactionDateDesc(pageable)).thenReturn(page);
        when(receiptRepository.findByTransaction_Id(1L)).thenReturn(Optional.empty());

        // Act
        org.springframework.data.domain.Page<TuitionTransactionDTO> result = 
                transactionService.getAllTransactions(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    // --- getTransactionById tests ---

    @Test
    void getTransactionById_Success() {
        // Arrange
        TuitionTransaction transaction = new TuitionTransaction();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("1000000"));
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        // Act
        TuitionTransaction result = transactionService.getTransactionById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getTransactionById_NotFound() {
        // Arrange
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.getTransactionById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Giao dịch không tồn tại");
    }

    // --- processPayment additional tests ---

    @Test
    void processPayment_NullStudentId() {
        // Arrange
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(null);
        request.setAmount(new BigDecimal("1000000"));

        // Act & Assert
        assertThatThrownBy(() -> transactionService.processPayment(request, collector))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Thông tin thanh toán không hợp lệ");
    }

    @Test
    void processPayment_NullAmount() {
        // Arrange
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(null);

        // Act & Assert
        assertThatThrownBy(() -> transactionService.processPayment(request, collector))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Thông tin thanh toán không hợp lệ");
    }

    @Test
    void processPayment_AlreadyPaidWithNeededZero() {
        // Arrange
        // Schedule is PENDING but paidAmount >= expectedAmount (should be PAID but status not updated)
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        schedule1.setPaidAmount(new BigDecimal("1000000"));
        schedule1.setExpectedAmount(new BigDecimal("1000000")); // needed = 0

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(new BigDecimal("500000"));
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        // Schedule should be set to PAID and saved
        assertThat(schedule1.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);
        verify(scheduleRepository, times(1)).save(schedule1);
    }

    @Test
    void processPayment_AlreadyPaidStatus_ShouldSkip() {
        // Arrange
        // Schedule is already PAID - should be skipped
        schedule1.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);
        schedule1.setPaidAmount(new BigDecimal("1000000"));
        schedule1.setExpectedAmount(new BigDecimal("1000000"));

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(new BigDecimal("500000"));
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        // Schedule should remain PAID and not be saved (skipped)
        assertThat(schedule1.getStatus()).isEqualTo(StudentPaymentSchedule.PaymentStatus.PAID);
        verify(scheduleRepository, never()).save(schedule1);
    }

    @Test
    void processPayment_WithNote() {
        // Arrange
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPaymentMethod("CASH");
        request.setNote("Test note");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        TuitionReceipt receipt = transactionService.processPayment(request, collector);

        // Assert
        assertThat(receipt).isNotNull();
        assertThat(receipt.getNote()).isEqualTo("Test note");
        
        ArgumentCaptor<TuitionTransaction> txCaptor = ArgumentCaptor.forClass(TuitionTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getDescription()).isEqualTo("Test note");
    }

    @Test
    void processPayment_WithoutNote() {
        // Arrange
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPaymentMethod("BANK_TRANSFER");
        request.setNote(null);

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        transactionService.processPayment(request, collector);

        // Assert
        ArgumentCaptor<TuitionTransaction> txCaptor = ArgumentCaptor.forClass(TuitionTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getDescription()).contains("Thanh toán học phí - BANK_TRANSFER");
    }

    @Test
    void processPayment_ReceiptCodeGenerated() {
        // Arrange
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setStudentId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPaymentMethod("CASH");

        when(scheduleRepository.findByStudentTuition_Student_Id(1L)).thenReturn(Collections.singletonList(schedule1));
        when(transactionRepository.save(any(TuitionTransaction.class))).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any(TuitionReceipt.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        TuitionReceipt receipt = transactionService.processPayment(request, collector);

        // Assert
        assertThat(receipt).isNotNull();
        assertThat(receipt.getReceiptCode()).isNotNull();
        assertThat(receipt.getReceiptCode()).startsWith("REC-");
    }
}

