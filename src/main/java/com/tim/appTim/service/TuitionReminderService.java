package com.tim.appTim.service;

import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.StudentPaymentSchedule;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.StudentPaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TuitionReminderService {

    private static final Logger logger = LoggerFactory.getLogger(TuitionReminderService.class);

    @Autowired
    private StudentPaymentScheduleRepository scheduleRepository;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * ?")
    //@Scheduled(fixedRate = 10000) // Dùng dòng này nếu muốn test nhanh (1 phút chạy 1 lần)
    @Transactional
    public void sendOverdueReminders() {
        logger.info("Bắt đầu quét các khoản học phí quá hạn...");

        LocalDate today = LocalDate.now();
        List<StudentPaymentSchedule> overdueSchedules = scheduleRepository
                .findByStatusNotAndDueDateBefore(StudentPaymentSchedule.PaymentStatus.PAID, today);

        int count = 0;
        for (StudentPaymentSchedule schedule : overdueSchedules) {

            if (schedule.getStatus() != StudentPaymentSchedule.PaymentStatus.OVERDUE) {
                schedule.setStatus(StudentPaymentSchedule.PaymentStatus.OVERDUE);
                scheduleRepository.save(schedule);
            }

            User student = schedule.getStudentTuition().getStudent();
            String formattedDate = schedule.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            String title = "Nhắc nhở: Học phí quá hạn";
            String content = String.format(
                    "Bạn có khoản học phí đợt %d (%.0f VNĐ) đã quá hạn thanh toán từ ngày %s. Vui lòng đóng sớm.",
                    schedule.getInstallmentNumber(),
                    schedule.getExpectedAmount(),
                    formattedDate
            );

            try {
                notificationService.createNotification(
                        student.getId(),
                        null,
                        Notification.NotificationType.TUITION_OVERDUE,
                        "PAYMENT_SCHEDULE",
                        schedule.getId(),
                        title,
                        content
                );
                count++;
            } catch (Exception e) {
                logger.error("Lỗi gửi thông báo cho SV {}: {}", student.getId(), e.getMessage());
            }
        }

        logger.info("Đã gửi {} thông báo quá hạn.", count);
    }
}