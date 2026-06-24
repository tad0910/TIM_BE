package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

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
import java.util.Map;
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
    private StudentPaymentScheduleHistoryRepository scheduleHistoryRepository;
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
        assertEquals(0, new BigDecimal("10500000").compareTo(result.getTotalActualFee()));

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
        assertEquals(0, new BigDecimal("9500000").compareTo(result.getTotalActualFee()));
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

        assertEquals(0, new BigDecimal("4000000").compareTo(schedule.getExpectedAmount()));
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

        assertEquals(0, new BigDecimal("2000000").compareTo(schedule.getExpectedAmount()));
    }

    // --- registerStudent additional tests ---

    @Test
    void testRegisterStudent_StudentNotFound() {
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), null);
        });
    }

    @Test
    void testRegisterStudent_RouteNotFound() {
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), null);
        });
    }

    @Test
    void testRegisterStudent_WithCoupon_Percent() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST10");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.PERCENT);
        coupon.setDiscountValue(new BigDecimal("10")); // 10% discount
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST10")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST10");

        // 10M - 1M (10%) + 500k = 9.5M
        assertEquals(0, new BigDecimal("9500000").compareTo(result.getTotalActualFee()));
        verify(couponRepository, times(1)).save(coupon);
    }

    @Test
    void testRegisterStudent_WithCoupon_InvalidCode() {
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "INVALID");
        });
    }

    @Test
    void testRegisterStudent_WithCoupon_NotActive() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(false);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");
        });
    }

    @Test
    void testRegisterStudent_WithCoupon_NotStarted() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().plusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(10));

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");
        });
    }

    @Test
    void testRegisterStudent_WithCoupon_Expired() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(10));
        coupon.setEndDate(LocalDate.now().minusDays(1));

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");
        });
    }

    @Test
    void testRegisterStudent_WithCoupon_OutOfQuantity() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setQuantity(10);
        coupon.setUsedCount(10);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");
        });
    }

    @Test
    void testRegisterStudent_WithCoupon_DiscountExceedsTuition() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setDiscountValue(new BigDecimal("20000000")); // 20M discount > 10M tuition
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");

        // Discount capped at tuition fee: 10M - 10M + 500k = 500k
        assertEquals(0, new BigDecimal("500000").compareTo(result.getTotalActualFee()));
    }

    @Test
    void testRegisterStudent_NoAdmissionFee() {
        route.setAdmissionFee(null);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), null);

        // 10M tuition + 0 admission = 10M
        assertEquals(0, new BigDecimal("10000000").compareTo(result.getTotalActualFee()));
        // Verify only 2 installments saved (no admission schedule)
        verify(paymentScheduleRepository, times(1)).saveAll(argThat(list -> {
            if (list instanceof List) {
                return ((List<?>) list).size() == 2;
            }
            return false;
        }));
    }

    @Test
    void testRegisterStudent_NoInstallmentConfigs_GenerateFromFrequency() {
        route.setInstallmentConfigs(null);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Service should generate configs from frequency when configs is null
        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), null);

        assertNotNull(result);
        // Verify schedules saved: 1 admission + 2 installments = 3 schedules
        verify(paymentScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testRegisterStudent_WithCoupon_SpreadEvenly() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setDiscountValue(new BigDecimal("1000000"));
        coupon.setScenario(Coupon.CouponScenario.SPREAD_EVENLY);
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");

        assertNotNull(result);
        verify(paymentScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testRegisterStudent_WithCoupon_DeductFirstFull() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setDiscountValue(new BigDecimal("1000000"));
        coupon.setScenario(Coupon.CouponScenario.DEDUCT_FIRST_FULL);
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");

        assertNotNull(result);
        verify(paymentScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testRegisterStudent_WithCoupon_DeductLastFull() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setDiscountValue(new BigDecimal("1000000"));
        coupon.setScenario(Coupon.CouponScenario.DEDUCT_LAST_FULL);
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");

        assertNotNull(result);
        verify(paymentScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testRegisterStudent_WithCoupon_PartialFirstThenSpread() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setDiscountValue(new BigDecimal("1000000"));
        coupon.setScenario(Coupon.CouponScenario.PARTIAL_FIRST_THEN_SPREAD);
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");

        assertNotNull(result);
        verify(paymentScheduleRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testRegisterStudent_WithCoupon_EmptyCode() {
        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "   ");

        assertNotNull(result);
        verify(couponRepository, never()).findByCode(anyString());
    }

    @Test
    void testRegisterStudent_FinalTuitionFeeNegative() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST100");
        coupon.setActive(true);
        coupon.setStartDate(LocalDate.now().minusDays(1));
        coupon.setEndDate(LocalDate.now().plusDays(1));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setDiscountValue(new BigDecimal("15000000")); // 15M discount > 10M tuition
        coupon.setQuantity(10);
        coupon.setUsedCount(0);

        when(studentTuitionRepository.existsByStudentIdAndTuitionRouteId(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(couponRepository.findByCode("TEST100")).thenReturn(Optional.of(coupon));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentTuition result = studentTuitionService.registerStudent(1L, 1L, LocalDate.now(), "TEST100");

        // finalTuitionFee = 10M - 10M (capped) = 0, + 500k admission = 500k
        assertEquals(0, new BigDecimal("500000").compareTo(result.getTotalActualFee()));
    }

    // --- adjustRemainingFee additional tests ---

    @Test
    void testAdjustRemainingFee_StudentTuitionNotFound() {
        when(studentTuitionRepository.findById(999L)).thenReturn(Optional.empty());

        FeeAdjustmentDTO dto = new FeeAdjustmentDTO();
        dto.setStudentTuitionId(999L);
        dto.setAmount(new BigDecimal("1000000"));

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.adjustRemainingFee(dto);
        });
    }

    @Test
    void testAdjustRemainingFee_NoPendingSchedules() {
        when(studentTuitionRepository.findById(1L)).thenReturn(Optional.of(studentTuition));
        when(paymentScheduleRepository.findByStudentTuitionIdAndStatus(1L,
                StudentPaymentSchedule.PaymentStatus.PENDING))
                .thenReturn(new ArrayList<>());

        FeeAdjustmentDTO dto = new FeeAdjustmentDTO();
        dto.setStudentTuitionId(1L);
        dto.setAmount(new BigDecimal("1000000"));

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.adjustRemainingFee(dto);
        });
    }

    @Test
    void testAdjustRemainingFee_NegativeAmount() {
        List<StudentPaymentSchedule> schedules = new ArrayList<>();
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setExpectedAmount(new BigDecimal("1000000"));
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        schedules.add(schedule);

        studentTuition.setPaymentSchedules(schedules);

        when(studentTuitionRepository.findById(1L)).thenReturn(Optional.of(studentTuition));
        when(paymentScheduleRepository.findByStudentTuitionIdAndStatus(1L,
                StudentPaymentSchedule.PaymentStatus.PENDING))
                .thenReturn(schedules);

        FeeAdjustmentDTO dto = new FeeAdjustmentDTO();
        dto.setStudentTuitionId(1L);
        dto.setAmount(new BigDecimal("-2000000")); // Would make it negative

        assertThrows(RuntimeException.class, () -> {
            studentTuitionService.adjustRemainingFee(dto);
        });
    }

    // --- updateScheduleDueDate tests ---

    @Test
    void testUpdateScheduleDueDate_Success() {
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        schedule.setFromDate(LocalDate.now().minusDays(10));
        schedule.setDueDate(LocalDate.now().plusDays(5));

        when(paymentScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(paymentScheduleRepository.save(any(StudentPaymentSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(scheduleHistoryRepository.save(any(StudentPaymentScheduleHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        LocalDate newDueDate = LocalDate.now().plusDays(10);
        studentTuitionService.updateScheduleDueDate(1L, newDueDate, "Test reason", 1L);

        assertEquals(newDueDate, schedule.getDueDate());
        verify(paymentScheduleRepository, times(1)).save(schedule);
    }

    @Test
    void testUpdateScheduleDueDate_NullDate() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            studentTuitionService.updateScheduleDueDate(1L, null, "Test reason", 1L);
        });
    }

    @Test
    void testUpdateScheduleDueDate_ScheduleNotFound() {
        when(paymentScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            studentTuitionService.updateScheduleDueDate(999L, LocalDate.now().plusDays(10), "Test reason", 1L);
        });
    }

    @Test
    void testUpdateScheduleDueDate_AlreadyPaid() {
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PAID);

        when(paymentScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            studentTuitionService.updateScheduleDueDate(1L, LocalDate.now().plusDays(10), "Test reason", 1L);
        });
    }

    @Test
    void testUpdateScheduleDueDate_AlreadyCancelled() {
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.CANCELLED);

        when(paymentScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            studentTuitionService.updateScheduleDueDate(1L, LocalDate.now().plusDays(10), "Test reason", 1L);
        });
    }

    @Test
    void testUpdateScheduleDueDate_BeforeFromDate() {
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        schedule.setFromDate(LocalDate.now().plusDays(10));

        when(paymentScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            studentTuitionService.updateScheduleDueDate(1L, LocalDate.now(), "Test reason", 1L);
        });
    }

    @Test
    void testUpdateScheduleDueDate_OverdueToPending() {
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.OVERDUE);
        schedule.setFromDate(LocalDate.now().minusDays(10));
        schedule.setDueDate(LocalDate.now().minusDays(5));

        when(paymentScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(paymentScheduleRepository.save(any(StudentPaymentSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(scheduleHistoryRepository.save(any(StudentPaymentScheduleHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        LocalDate newDueDate = LocalDate.now().plusDays(5);
        studentTuitionService.updateScheduleDueDate(1L, newDueDate, "Test reason", 1L);

        assertEquals(StudentPaymentSchedule.PaymentStatus.PENDING, schedule.getStatus());
    }

    @Test
    void testUpdateScheduleDueDate_PendingToOverdue() {
        StudentPaymentSchedule schedule = new StudentPaymentSchedule();
        schedule.setId(1L);
        schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        schedule.setFromDate(LocalDate.now().minusDays(10));
        schedule.setDueDate(LocalDate.now().plusDays(5));

        when(paymentScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(paymentScheduleRepository.save(any(StudentPaymentSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(scheduleHistoryRepository.save(any(StudentPaymentScheduleHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        LocalDate newDueDate = LocalDate.now().minusDays(1);
        studentTuitionService.updateScheduleDueDate(1L, newDueDate, "Test reason", 1L);

        assertEquals(StudentPaymentSchedule.PaymentStatus.OVERDUE, schedule.getStatus());
    }

    // --- batchRegisterByProgram tests ---

    @Test
    void testBatchRegisterByProgram_Success() {
        Programs program = new Programs();
        program.setId(1);

        User student1 = new User();
        student1.setId(1L);
        User student2 = new User();
        student2.setId(2L);

        ClassMember member1 = new ClassMember();
        member1.setUser(student1);
        ClassMember member2 = new ClassMember();
        member2.setUser(student2);

        List<ClassMember> members = List.of(member1, member2);

        when(classMemberRepository.findStudentsByProgramId(1L)).thenReturn(members);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRoute_ProgramId(1L, 1L)).thenReturn(false);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRoute_ProgramId(2L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = studentTuitionService.batchRegisterByProgram(1L, 1L, LocalDate.now());

        assertEquals(2, result.get("totalStudents"));
        assertEquals(2, result.get("success"));
        assertEquals(0, result.get("skipped"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");
        assertTrue(errors.isEmpty());
    }

    @Test
    void testBatchRegisterByProgram_SomeAlreadyRegistered() {
        Programs program = new Programs();
        program.setId(1);

        User student1 = new User();
        student1.setId(1L);
        User student2 = new User();
        student2.setId(2L);

        ClassMember member1 = new ClassMember();
        member1.setUser(student1);
        ClassMember member2 = new ClassMember();
        member2.setUser(student2);

        List<ClassMember> members = List.of(member1, member2);

        when(classMemberRepository.findStudentsByProgramId(1L)).thenReturn(members);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRoute_ProgramId(1L, 1L)).thenReturn(true);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRoute_ProgramId(2L, 1L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = studentTuitionService.batchRegisterByProgram(1L, 1L, LocalDate.now());

        assertEquals(2, result.get("totalStudents"));
        assertEquals(1, result.get("success"));
        assertEquals(1, result.get("skipped"));
    }

    @Test
    void testBatchRegisterByProgram_SomeErrors() {
        Programs program = new Programs();
        program.setId(1);

        User student1 = new User();
        student1.setId(1L);
        User student2 = new User();
        student2.setId(2L);

        ClassMember member1 = new ClassMember();
        member1.setUser(student1);
        ClassMember member2 = new ClassMember();
        member2.setUser(student2);

        List<ClassMember> members = List.of(member1, member2);

        when(classMemberRepository.findStudentsByProgramId(1L)).thenReturn(members);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRoute_ProgramId(1L, 1L)).thenReturn(false);
        when(studentTuitionRepository.existsByStudentIdAndTuitionRoute_ProgramId(2L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(userRepository.findById(2L)).thenReturn(Optional.empty()); // Student not found
        when(tuitionRouteRepository.findById(1L)).thenReturn(Optional.of(route));
        when(studentTuitionRepository.save(any(StudentTuition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = studentTuitionService.batchRegisterByProgram(1L, 1L, LocalDate.now());

        assertEquals(2, result.get("totalStudents"));
        assertEquals(1, result.get("success"));
        assertEquals(0, result.get("skipped"));
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");
        assertFalse(errors.isEmpty());
    }

    @Test
    void testBatchRegisterByProgram_EmptyMembers() {
        when(classMemberRepository.findStudentsByProgramId(1L)).thenReturn(new ArrayList<>());

        Map<String, Object> result = studentTuitionService.batchRegisterByProgram(1L, 1L, LocalDate.now());

        assertEquals(0, result.get("totalStudents"));
        assertEquals(0, result.get("success"));
        assertEquals(0, result.get("skipped"));
    }

}

