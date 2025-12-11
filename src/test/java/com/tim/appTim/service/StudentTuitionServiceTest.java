package com.tim.appTim.service;

import com.tim.appTim.dto.FeeAdjustmentDTO;
import com.tim.appTim.dto.PaymentRequestDTO;
import com.tim.appTim.entity.*;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentTuitionServiceTest {

    @Mock
    private StudentTuitionRepository studentTuitionRepository;
    @Mock
    private StudentPaymentScheduleRepository paymentScheduleRepository;
    @Mock
    private TuitionRouteRepository tuitionRouteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private ClassMemberRepository classMemberRepository;

    @InjectMocks
    private StudentTuitionService studentTuitionService;

    private User student;
    private TuitionRoute route;
    private StudentTuition studentTuition;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1L);
        student.setUsername("student1");

        route = new TuitionRoute();
        route.setId(1L);
        route.setTotalListedFee(new BigDecimal("10000000"));
        route.setNumberOfInstallments(2);
        route.setFrequency(1); // 1 month
        route.setAdmissionFee(new BigDecimal("500000"));

        // Mock installment configs
        List<TuitionInstallmentConfig> configs = new ArrayList<>();
        TuitionInstallmentConfig config1 = new TuitionInstallmentConfig();
        config1.setInstallmentNumber(1);
        config1.setBaseAmount(new BigDecimal("5000000"));
        config1.setDaysFromPrevious(30);
        configs.add(config1);

        TuitionInstallmentConfig config2 = new TuitionInstallmentConfig();
        config2.setInstallmentNumber(2);
        config2.setBaseAmount(new BigDecimal("5000000"));
        config2.setDaysFromPrevious(30);
        configs.add(config2);

        route.setInstallmentConfigs(configs);

        studentTuition = new StudentTuition();
        studentTuition.setId(1L);
        studentTuition.setStudent(student);
        studentTuition.setTuitionRoute(route);
        studentTuition.setTotalActualFee(new BigDecimal("10500000"));
    }

    @Test
    void testRegisterStudent_Success() {
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), null);

        assertNotNull(result);
        assertEquals(student, result.getStudent());
        assertEquals(route, result.getTuitionRoute());
        // 10M tuition + 500k admission = 10.5M
        assertEquals(new BigDecimal("10500000"), result.getTotalActualFee());

        // Verify schedules saved: 1 admission + 2 installments = 3 schedules
        verify(paymentScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testRegisterStudent_WithCoupon_Amount() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setDiscountValue(new BigDecimal("1000000")); // 1M discount
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");

        // 10M - 1M + 500k = 9.5M
        assertEquals(new BigDecimal("9500000"), result.getTotalActualFee());
        verify(couponRepository, times(1)).save(coupon);
        assertEquals(1, coupon.getUsedCount());
    }

    @Test
    void testRegisterStudent_AlreadyRegistered() {
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), null);
        });
    }

    @Test
    void testAdjustRemainingFee_Success() {
        List<StudentPaymentSchedule> schedules = new ArrayList<>();
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setExpectedAmount(new BigDecimal("5000000"));
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        schedules.add(schedule);

        // Mock getPaymentSchedules for updateTotalActualFee
        studentTuition.setPaymentSchedules(schedules);

        when(studentTuitionRepository.findById(1L)).thenReturn(Optional.of(studentTuition));
        when(paymentScheduleRepository.findByStudentTuitionIdAndStatus(1L,
                StudentPaymentSchedule.PaymentStatus.PENDING))
                .thenReturn(schedules);

        FeeAdjustmentDTO dto = new FeeAdjustmentDTO();
        dto.setStudentTuitionId(1L);
        dto.setAmount(new BigDecimal("-1000000")); // Reduce by 1M
        dto.setFixedAmount(false);

        studentTuitionService.adjustRemainingFee(dto);

        assertEquals(new BigDecimal("4000000"), schedule.getExpectedAmount());
        verify(paymentScheduleRepository, times(1)).saveAll(schedules);
        verify(studentTuitionRepository, times(1)).save(studentTuition);
    }

    @Test
    void testAdjustRemainingFee_FixedAmount() {
        List<StudentPaymentSchedule> schedules = new ArrayList<>();
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setExpectedAmount(new BigDecimal("5000000"));
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        schedules.add(schedule);

        studentTuition.setPaymentSchedules(schedules);

        when(studentTuitionRepository.findById(1L)).thenReturn(Optional.of(studentTuition));
        when(paymentScheduleRepository.findByStudentTuitionIdAndStatus(1L,
                StudentPaymentSchedule.PaymentStatus.PENDING))
                .thenReturn(schedules);

        FeeAdjustmentDTO dto = new FeeAdjustmentDTO();
        dto.setStudentTuitionId(1L);
        dto.setAmount(new BigDecimal("2000000")); // Set to 2M
        dto.setFixedAmount(true);

        studentTuitionService.adjustRemainingFee(dto);

        assertEquals(new BigDecimal("2000000"), schedule.getExpectedAmount());
    }

}
