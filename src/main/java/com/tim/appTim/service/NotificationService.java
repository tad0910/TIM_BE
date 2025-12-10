package com.tim.appTim.service;

import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.dto.NotificationDTO;
import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.AttendanceSessionRepository;
import com.tim.appTim.repository.NotificationRepository;
import com.tim.appTim.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.List;

@Service("notificationService")
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SseService sseService;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public NotificationService(NotificationRepository notificationRepository, UserService userService, UserRepository userRepository, SseService sseService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.sseService = sseService;
    }

    public NotificationDTO createNotification(Long receiverId, Long senderId,
                                              Notification.NotificationType notificationType,
                                              String targetType, Long targetId,
                                              String title, String content) {
        return createNotification(receiverId, senderId, notificationType, targetType, targetId, title, content, null);
    }

    public NotificationDTO createNotification(Long receiverId, Long senderId,
                                              Notification.NotificationType notificationType,
                                              String targetType, Long targetId,
                                              String title, String content,
                                              String iconUrl) {

        boolean shouldCheckDuplicate =
                senderId != null
                        && notificationType != Notification.NotificationType.GRADE_NEW
                        && notificationType != Notification.NotificationType.GRADE_UPDATED;

        if (shouldCheckDuplicate
                && notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                receiverId, senderId, notificationType, targetType, targetId)) {
            return null;
        }

        Notification notification = new Notification(receiverId, senderId, notificationType,
                targetType, targetId, title, content, iconUrl);

        Notification savedNotification = notificationRepository.save(notification);
        NotificationDTO notificationDTO = convertToDTO(savedNotification);

        sseService.sendNotification(receiverId, notificationDTO);

        return notificationDTO;
    }

    public Page<NotificationDTO> getNotificationsByUserId(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::convertToDTO);
    }

    public Page<NotificationDTO> getUnreadNotificationsByUserId(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::convertToDTO);
    }

    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByReceiverId(userId);
    }

    public void markAsRead(Long notificationId, Long currentUserId, Authentication authentication) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại với ID: " + notificationId));

        if (!notification.getReceiver().getId().equals(currentUserId)) {
            throw new ForbiddenException("User not authorized to mark this notification as read");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    public void deleteOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotifications(cutoffDate);
    }

    public Page<NotificationDTO> getNotificationsByType(Long userId, Notification.NotificationType notificationType, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReceiverIdAndNotificationTypeOrderByCreatedAtDesc(userId, notificationType, pageable);
        return notifications.map(this::convertToDTO);
    }

    public void createReactionNotification(Long postOwnerId, Long commentOwnerId, Long replyOwnerId,
                                           Long senderId, String senderUsername,
                                           Notification.NotificationType notificationType,
                                           String targetType, Long targetId) {

        if (senderId == null) return;

        Long receiverId = null;
        String title = "";
        String content = "";

        switch (notificationType) {
            case POST_REACTION:
                receiverId = postOwnerId;
                title = "Bài viết của bạn được bày tỏ cảm xúc";
                content = senderUsername + " đã bày tỏ cảm xúc về bài viết của bạn";
                break;
            case COMMENT_REACTION:
                receiverId = commentOwnerId;
                title = "Bình luận của bạn được bày tỏ cảm xúc";
                content = senderUsername + " đã bày tỏ cảm xúc về bình luận của bạn";
                break;
            case REPLY_REACTION:
                receiverId = replyOwnerId;
                title = "Phản hồi của bạn được bày tỏ cảm xúc";
                content = senderUsername + " đã bày tỏ cảm xúc về phản hồi của bạn";
                break;
            case POST_COMMENT:
            case COMMENT_REPLY:
            case POST_MENTION:
            case COMMENT_MENTION:
            case USER_FOLLOW:
            case SYSTEM_ANNOUNCEMENT:
            default:
                break;
        }

        if (receiverId != null && !receiverId.equals(senderId)) {
            createNotification(receiverId, senderId, notificationType, targetType, targetId, title, content);
        }
    }

    public void createCommentNotification(Long postOwnerId, Long commentOwnerId, Long senderId,
                                          String senderUsername, String targetType, Long targetId) {
        if (senderId == null) return;

        Long receiverId = null;
        String title = "";
        String content = "";

        if (targetType.equals("POST")) {
            receiverId = postOwnerId;
            title = "Bài viết của bạn có bình luận mới";
            content = senderUsername + " đã bình luận bài viết của bạn";
        } else if (targetType.equals("COMMENT")) {
            receiverId = commentOwnerId;
            title = "Bình luận của bạn có phản hồi mới";
            content = senderUsername + " đã phản hồi bình luận của bạn";
        }

        if (receiverId != null && !receiverId.equals(senderId)) {
            createNotification(receiverId, senderId,
                    targetType.equals("POST") ? Notification.NotificationType.POST_COMMENT : Notification.NotificationType.COMMENT_REPLY,
                    targetType, targetId, title, content);
        }
    }

    private NotificationDTO convertToDTO(Notification notification) {
        String senderUsername = "Hệ thống";
        String senderAvatar = null;

        if (notification.getSender() == null && notification.getSenderId() != null) {
            User senderUser = userRepository.findById(notification.getSenderId()).orElse(null);
            notification.setSender(senderUser);
        }

        if (notification.getSender() != null) {
            senderUsername = notification.getSender().getUsername();
            senderAvatar = notification.getSender().getProfileImage();
        }

        String actionUrl = generateActionUrl(notification);

        return new NotificationDTO(
                notification.getId(),
                notification.getReceiverId(),
                notification.getSenderId(),
                senderUsername,
                senderAvatar,
                notification.getNotificationType().name(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getIconUrl(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                actionUrl
        );
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void remindTeachersToOpenAttendance() {
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> activeSchedules = getActiveSchedulesForReminder(now);

        for (Object[] row : activeSchedules) {
            Long scheduleId = ((Number) row[0]).longValue();
            Integer teacherId = row[1] instanceof Number ? ((Number) row[1]).intValue() : null;
            LocalDateTime startDate = row[2] instanceof java.sql.Timestamp
                    ? ((java.sql.Timestamp) row[2]).toLocalDateTime() : null;
            LocalDateTime endDate = row[3] instanceof java.sql.Timestamp
                    ? ((java.sql.Timestamp) row[3]).toLocalDateTime() : null;
            String moduleInfo = (String) row[4];

            if (teacherId == null || startDate == null || endDate == null) continue;

            boolean isOpened = sessionRepository.findByScheduleId(scheduleId).isPresent();
            if (isOpened) continue;

            String title = null;
            String content = null;
            Notification.NotificationType type = null;

            if (now.isAfter(startDate.plusMinutes(15)) && now.isBefore(endDate)) {
                title = "Bạn chưa mở điểm danh";
                content = String.format("Buổi học %s đã bắt đầu hơn 15 phút nhưng chưa được mở điểm danh.", moduleInfo);
                type = Notification.NotificationType.ATTENDANCE_REMINDER_LATE;
            }

            else if (now.isAfter(endDate.minusMinutes(10)) && now.isBefore(endDate.plusMinutes(1))) {
                title = "Bạn chưa điểm danh cho buổi học này";
                content = String.format("Buổi học %s sắp kết thúc (còn 10 phút) nhưng chưa được điểm danh.", moduleInfo);
                type = Notification.NotificationType.ATTENDANCE_REMINDER_ENDING;
            }

            if (type != null && !notificationAlreadySent(teacherId.longValue(), scheduleId, title)) {
                createNotification(
                        teacherId.longValue(),
                        null,
                        type,
                        "ATTENDANCE_SCHEDULE",
                        scheduleId,
                        title,
                        content
                );
            }
        }
    }

    private boolean notificationAlreadySent(Long receiverId, Long targetId, String title) {
        return notificationRepository.existsByReceiverIdAndTargetTypeAndTargetIdAndTitle(
                receiverId, "ATTENDANCE_SCHEDULE", targetId, title
        );
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> getActiveSchedulesForReminder(LocalDateTime now) {
        String sql = """
            SELECT DISTINCT
                cms.id,
                COALESCE(cms.instructor_id, cmst.user_id) AS teacher_id,
                cms.start_date,
                cms.end_date,
                CONCAT(
                    COALESCE(m.name, 'Module'),
                    ' - Buổi ',
                    COALESCE(ms.session_number, '#')
                ) AS module_info
            FROM class_module_schedules cms
            LEFT JOIN class_module_schedule_teacher cmst 
                   ON cmst.class_module_schedule_id = cms.id
            LEFT JOIN modules m ON cms.module_id = m.id
            LEFT JOIN module_sessions ms ON cms.module_session_id = ms.id
            WHERE cms.start_date <= ? 
              AND cms.end_date >= ?
              AND COALESCE(cms.instructor_id, cmst.user_id) IS NOT NULL
            """;

        return entityManager.createNativeQuery(sql)
                .setParameter(1, now.plusMinutes(15))
                .setParameter(2, now.minusMinutes(10))
                .getResultList();
    }

    private String generateActionUrl(Notification notification) {
        String baseUrl = "/";

        switch (notification.getNotificationType()) {
            case POST_REACTION:
            case POST_COMMENT:
                return baseUrl + "posts/" + notification.getTargetId();
            case COMMENT_REACTION:
            case COMMENT_REPLY:
                return baseUrl + "posts/" + getPostIdFromComment(notification.getTargetId());
            case REPLY_REACTION:
                return baseUrl + "posts/" + getPostIdFromReply(notification.getTargetId());
            case USER_FOLLOW:
                return baseUrl + "users/" + notification.getSenderId();
            case LATE_ATTENDANCE_OPENED:
                return baseUrl + "attendance/schedule/" + notification.getTargetId();
            case ATTENDANCE_REMINDER_LATE:
                return baseUrl + "attendance/schedule/" + notification.getTargetId();
            case ATTENDANCE_REMINDER_ENDING:
                return baseUrl + "attendance/schedule/" + notification.getTargetId();
            case GRADE_NEW, GRADE_UPDATED:
                return baseUrl + "class-modules/" + notification.getTargetId() + "/my-grades";

            case BLOG_NEW:
                return baseUrl + "news";

            case TUITION_OVERDUE:
                return baseUrl + "tuition/my-overview";

            default:
                return baseUrl;
        }
    }

    private Long getPostIdFromComment(Long commentId) {
        return null;
    }

    private Long getPostIdFromReply(Long replyId) {
        return null;
    }

    public boolean isReceiver(Authentication authentication, Long notificationId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) return false;

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy Notification: " + notificationId));

        return notification.getReceiver().getId().equals(currentUser.getId());
    }
}