package com.tim.appTim.service;

import com.tim.appTim.dto.PaymentRequestDTO;
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
}
