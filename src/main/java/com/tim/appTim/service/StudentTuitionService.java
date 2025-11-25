package com.tim.appTim.service;

import com.tim.appTim.dto.FeeAdjustmentDTO;
import com.tim.appTim.entity.*;
import com.tim.appTim.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StudentTuitionService {

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
        if (finalTuitionFee.compareTo(BigDecimal.ZERO) < 0) finalTuitionFee = BigDecimal.ZERO;

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
        int frequencyMonths = route.getFrequency();

        BigDecimal baseAmountPerInstallment = route.getTotalListedFee().divide(
                BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);

        LocalDate currentFromDate = enrollmentDate;

        for (int i = 1; i <= totalInstallments; i++) {
            StudentPaymentSchedule schedule = new StudentPaymentSchedule();
            schedule.setStudentTuition(profile);
            schedule.setInstallmentNumber(i);
            schedule.setPaidAmount(BigDecimal.ZERO);
            schedule.setStatus(StudentPaymentSchedule.PaymentStatus.PENDING);

            schedule.setFromDate(currentFromDate);
            LocalDate dueDate = currentFromDate.plusMonths(frequencyMonths);
            schedule.setDueDate(dueDate);
            currentFromDate = dueDate;

            BigDecimal discountForThisInstallment = BigDecimal.ZERO;

            if (coupon != null) {
                Coupon.CouponScenario scenario = coupon.getScenario();
                if (scenario == null) scenario = Coupon.CouponScenario.SPREAD_EVENLY;

                switch (scenario) {
                    case SPREAD_EVENLY:
                        discountForThisInstallment = totalDiscountAmount.divide(
                                BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
                        break;

                    case DEDUCT_FIRST_FULL:
                        if (i == 1) {
                            discountForThisInstallment = totalDiscountAmount;
                        }
                        break;

                    case DEDUCT_LAST_FULL:
                        if (i == totalInstallments) {
                            discountForThisInstallment = totalDiscountAmount;
                        }
                        break;

                    case PARTIAL_FIRST_THEN_SPREAD:
                        BigDecimal firstPart = totalDiscountAmount.multiply(new BigDecimal("0.5"));
                        BigDecimal restPart = totalDiscountAmount.subtract(firstPart);

                        if (i == 1) {
                            if (totalInstallments > 1) {
                                discountForThisInstallment = firstPart;
                            } else {
                                discountForThisInstallment = totalDiscountAmount;
                            }
                        } else {
                            discountForThisInstallment = restPart.divide(
                                    BigDecimal.valueOf(totalInstallments - 1), 2, RoundingMode.HALF_UP);
                        }
                        break;
                }
            }

            BigDecimal finalAmount = baseAmountPerInstallment.subtract(discountForThisInstallment);

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
            throw new RuntimeException("Sinh viên này đã hoàn thành học phí hoặc không còn đợt nào trạng thái PENDING để điều chỉnh.");
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

        BigDecimal newTotal = BigDecimal.ZERO;
        for (StudentPaymentSchedule sch : allSchedules) {
            newTotal = newTotal.add(sch.getExpectedAmount());
        }

        profile.setTotalActualFee(newTotal);
        studentTuitionRepository.save(profile);
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
                "errors", errors
        );
    }
}
