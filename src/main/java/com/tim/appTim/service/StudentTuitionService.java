package com.tim.appTim.service;

import com.tim.appTim.dto.FeeAdjustmentDTO;
import com.tim.appTim.entity.*;
import com.tim.appTim.entity.TuitionInstallmentConfig;
import com.tim.appTim.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StudentTuitionService {

    private static final Logger logger = LoggerFactory.getLogger(StudentTuitionService.class);

    @Autowired
    private StudentTuitionRepository studentTuitionRepository;
    @Autowired
    private StudentPaymentScheduleRepository paymentScheduleRepository;
    @Autowired
    private TuitionRouteRepository tuitionRouteRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ClassMemberRepository classMemberRepository;

    @Transactional
    public StudentTuition registerStudent(Long studentId, Long routeId, LocalDate enrollmentDate, String couponCode) {

        if (studentTuitionRepository.existsByStudentIdAndTuitionRouteId(studentId, routeId)) {
            throw new RuntimeException("Sinh viên này đã đăng ký lộ trình học phí này rồi!");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Sinh viên không tồn tại với ID: " + studentId));

        TuitionRoute route = tuitionRouteRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Lộ trình học phí không tồn tại với ID: " + routeId));

        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (couponCode != null && !couponCode.trim().isEmpty()) {
            coupon = couponRepository.findByCode(couponCode)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ: " + couponCode));

            if (!coupon.isActive()) {
                throw new RuntimeException("Mã giảm giá đã ngừng hoạt động.");
            }
            if (LocalDate.now().isBefore(coupon.getStartDate()) || LocalDate.now().isAfter(coupon.getEndDate())) {
                throw new RuntimeException("Mã giảm giá chưa bắt đầu hoặc đã hết hạn.");
            }
            if (coupon.getQuantity() != null && coupon.getUsedCount() >= coupon.getQuantity()) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng.");
            }

            BigDecimal tuitionFee = route.getTotalListedFee();
            if (coupon.getDiscountType() == Coupon.DiscountType.AMOUNT) {
                totalDiscountAmount = coupon.getDiscountValue();
            } else {
                totalDiscountAmount = tuitionFee.multiply(coupon.getDiscountValue())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }

            if (totalDiscountAmount.compareTo(tuitionFee) > 0) {
                totalDiscountAmount = tuitionFee;
            }

            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        StudentTuition profile = new StudentTuition();
        profile.setStudent(student);
        profile.setTuitionRoute(route);
        profile.setEnrollmentDate(enrollmentDate);
        profile.setStatus(StudentTuition.TuitionStatus.ACTIVE);

        BigDecimal finalTuitionFee = route.getTotalListedFee().subtract(totalDiscountAmount);
        if (finalTuitionFee.compareTo(BigDecimal.ZERO) < 0)
            finalTuitionFee = BigDecimal.ZERO;

        BigDecimal admissionFee = (route.getAdmissionFee() != null) ? route.getAdmissionFee() : BigDecimal.ZERO;
        profile.setTotalActualFee(finalTuitionFee.add(admissionFee));

        profile = studentTuitionRepository.save(profile);

        List<StudentPaymentSchedule> schedules = new ArrayList<>();

        if (admissionFee.compareTo(BigDecimal.ZERO) > 0) {
            StudentPaymentSchedule admissionSchedule = new StudentPaymentSchedule();
            admissionSchedule.setStudentTuition(profile);
            admissionSchedule.setInstallmentNumber(0);
            admissionSchedule.setExpectedAmount(admissionFee);
            admissionSchedule.setPaidAmount(BigDecimal.ZERO);
            admissionSchedule.setFromDate(enrollmentDate);
            admissionSchedule.setDueDate(enrollmentDate.plusDays(7));
            admissionSchedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);

            schedules.add(admissionSchedule);
        }

        int totalInstallments = route.getNumberOfInstallments();
        List<TuitionInstallmentConfig> configs = route.getInstallmentConfigs();

        if (configs == null || configs.isEmpty()) {
            configs = generateConfigsFromFrequency(route);
        }

        LocalDate currentDueDate = enrollmentDate;

        for (int i = 1; i <= totalInstallments; i++) {
            StudentPaymentSchedule schedule = new StudentPaymentSchedule();
            schedule.setStudentTuition(profile);
            schedule.setInstallmentNumber(i);
            schedule.setPaidAmount(BigDecimal.ZERO);
            schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);

            final int installmentNum = i;
            TuitionInstallmentConfig config = configs.stream()
                    .filter(c -> c.getInstallmentNumber().equals(installmentNum))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình cho kỳ " + installmentNum));

            schedule.setFromDate(currentDueDate);
            LocalDate dueDate = currentDueDate.plusDays(config.getDaysFromPrevious());
            schedule.setDueDate(dueDate);
            currentDueDate = dueDate;

            BigDecimal discountForThisInstallment = BigDecimal.ZERO;
            if (coupon != null) {
                discountForThisInstallment = calculateDiscountForInstallment(
                        coupon, i, totalInstallments, config.getBaseAmount(),
                        route.getTotalListedFee(), totalDiscountAmount);
            }

            BigDecimal finalAmount = config.getBaseAmount().subtract(discountForThisInstallment);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }

            schedule.setExpectedAmount(finalAmount);
            schedules.add(schedule);
        }

        paymentScheduleRepository.saveAll(schedules);
        return profile;
    }

    @Transactional
    public void adjustRemainingFee(FeeAdjustmentDTO dto) {

        StudentTuition profile = studentTuitionRepository.findById(dto.getStudentTuitionId())
                .orElseThrow(() -> new RuntimeException("Hồ sơ học phí không tồn tại"));

        List<StudentPaymentSchedule> pendingSchedules = paymentScheduleRepository
                .findByStudentTuitionIdAndStatus(profile.getId(), StudentPaymentSchedule.PaymentStatus.PENDING);

        if (pendingSchedules.isEmpty()) {
            throw new RuntimeException(
                    "Sinh viên này đã hoàn thành học phí hoặc không còn đợt nào trạng thái PENDING để điều chỉnh.");
        }

        for (StudentPaymentSchedule schedule : pendingSchedules) {
            BigDecimal currentAmount = schedule.getExpectedAmount();
            BigDecimal newAmount;

            if (dto.isFixedAmount()) {
                newAmount = dto.getAmount();
            } else {
                newAmount = currentAmount.add(dto.getAmount());
            }

            if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Lỗi: Số tiền sau điều chỉnh không được nhỏ hơn 0.");
            }

            schedule.setExpectedAmount(newAmount);
        }

        paymentScheduleRepository.saveAll(pendingSchedules);

        updateTotalActualFee(profile);
    }

    private void updateTotalActualFee(StudentTuition profile) {

        List<StudentPaymentSchedule> allSchedules = profile.getPaymentSchedules();
        if (allSchedules == null) {
            allSchedules = new ArrayList<>();
        }

        BigDecimal newTotal = BigDecimal.ZERO;
        for (StudentPaymentSchedule sch : allSchedules) {
            newTotal = newTotal.add(sch.getExpectedAmount());
        }

        profile.setTotalActualFee(newTotal);
        studentTuitionRepository.save(profile);
    }

    @Transactional
    public void updateScheduleDueDate(Long scheduleId, LocalDate newDueDate, String reason) {

        if (newDueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày hạn mới không hợp lệ");
        }

        StudentPaymentSchedule schedule = paymentScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch thanh toán với ID: " + scheduleId));

        StudentPaymentSchedule.PaymentStatus currentStatus = schedule.getStatus();
        if (currentStatus == StudentPaymentSchedule.PaymentStatus.PAID
                || currentStatus == StudentPaymentSchedule.PaymentStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể cập nhật hạn đóng cho đợt đã hoàn tất hoặc đã hủy.");
        }

        LocalDate fromDate = schedule.getFromDate();
        if (fromDate != null && newDueDate.isBefore(fromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày hạn mới không được trước ngày bắt đầu kỳ thanh toán.");
        }

        schedule.setDueDate(newDueDate);

        LocalDate today = LocalDate.now();
        if (schedule.getStatus() == StudentPaymentSchedule.PaymentStatus.OVERDUE
                && (newDueDate.isAfter(today) || newDueDate.isEqual(today))) {
            schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);
        } else if (schedule.getStatus() == StudentPaymentSchedule.PaymentStatus.PENDING
                && newDueDate.isBefore(today)) {
            schedule.setStatus(StudentPaymentSchedule.PaymentStatus.OVERDUE);
        }

        paymentScheduleRepository.save(schedule);

        if (logger.isInfoEnabled()) {
            logger.info("Đã cập nhật hạn đóng cho schedule {} -> {}. Reason: {}", scheduleId, newDueDate, reason);
        }
    }

    @Transactional
    public Map<String, Object> batchRegisterByProgram(Long programId, Long routeId, LocalDate enrollmentDate) {

        List<ClassMember> members = classMemberRepository.findStudentsByProgramId(programId);

        int successCount = 0;
        int skipCount = 0;
        List<String> errors = new ArrayList<>();

        for (ClassMember member : members) {
            Long studentId = member.getUser().getId();

            boolean alreadyHasTuition = studentTuitionRepository
                    .existsByStudentIdAndTuitionRoute_ProgramId(studentId, programId);

            if (alreadyHasTuition) {
                skipCount++;
                continue;
            }

            try {
                registerStudent(studentId, routeId, enrollmentDate, null);
                successCount++;
            } catch (Exception e) {
                errors.add("Lỗi SV ID " + studentId + ": " + e.getMessage());
            }
        }

        return Map.of(
                "totalStudents", members.size(),
                "success", successCount,
                "skipped", skipCount,
                "errors", errors);
    }

    private List<TuitionInstallmentConfig> generateConfigsFromFrequency(TuitionRoute route) {
        List<TuitionInstallmentConfig> configs = new ArrayList<>();
        int totalInstallments = route.getNumberOfInstallments();
        BigDecimal baseAmount = route.getTotalListedFee().divide(
                BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);

        int daysPerInstallment = route.getFrequency() * 30;

        for (int i = 1; i <= totalInstallments; i++) {
            TuitionInstallmentConfig config = new TuitionInstallmentConfig();
            config.setInstallmentNumber(i);
            config.setBaseAmount(baseAmount);
            config.setDaysFromPrevious(daysPerInstallment);
            configs.add(config);
        }

        return configs;
    }

    private BigDecimal calculateDiscountForInstallment(
            Coupon coupon, int installmentNumber, int totalInstallments,
            BigDecimal baseAmount, BigDecimal totalListedFee, BigDecimal totalDiscount) {
        Coupon.CouponScenario scenario = coupon.getScenario();
        if (scenario == null)
            scenario = Coupon.CouponScenario.SPREAD_EVENLY;

        switch (scenario) {
            case SPREAD_EVENLY:
                return totalDiscount.multiply(baseAmount)
                        .divide(totalListedFee, 2, RoundingMode.HALF_UP);

            case DEDUCT_FIRST_FULL:
                return (installmentNumber == 1) ? totalDiscount : BigDecimal.ZERO;

            case DEDUCT_LAST_FULL:
                return (installmentNumber == totalInstallments) ? totalDiscount : BigDecimal.ZERO;

            case PARTIAL_FIRST_THEN_SPREAD:
                BigDecimal firstPart = totalDiscount.multiply(new BigDecimal("0.5"));
                BigDecimal restPart = totalDiscount.subtract(firstPart);

                if (installmentNumber == 1) {
                    return firstPart;
                } else {
                    BigDecimal totalRemainingBase = totalListedFee.subtract(baseAmount);
                    if (totalRemainingBase.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                    }
                    return restPart.multiply(baseAmount)
                            .divide(totalRemainingBase, 2, RoundingMode.HALF_UP);
                }

            default:
                return BigDecimal.ZERO;
        }
    }
}
