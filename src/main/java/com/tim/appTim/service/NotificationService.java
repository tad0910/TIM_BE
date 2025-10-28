package com.tim.appTim.service;

import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.dto.NotificationDTO;
import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.NotificationRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

@Service("notificationService")
@Transactional
public class NotificationService {


    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository, UserService userService, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // Tạo thông báo mới
    public NotificationDTO createNotification(Long receiverId, Long senderId,
                                              Notification.NotificationType notificationType,
                                              String targetType, Long targetId,
                                              String title, String content) {

        // Kiểm tra xem đã có thông báo tương tự chưa (tránh spam)
        if (senderId != null && notificationRepository.existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
                receiverId, senderId, notificationType, targetType, targetId)) {
            return null; // Không tạo thông báo trùng lặp
        }

        Notification notification = new Notification(receiverId, senderId, notificationType,
                targetType, targetId, title, content);

        Notification savedNotification = notificationRepository.save(notification);
        return convertToDTO(savedNotification);
    }

    // Lấy danh sách thông báo của user với phân trang
    public Page<NotificationDTO> getNotificationsByUserId(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(this::convertToDTO);
    }

    // Lấy thông báo chưa đọc của user
    public List<NotificationDTO> getUnreadNotificationsByUserId(Long userId) {
        List<Notification> notifications = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Đếm số thông báo chưa đọc
    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    // Đánh dấu tất cả thông báo là đã đọc
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByReceiverId(userId);
    }

    // Đánh dấu một thông báo cụ thể là đã đọc
    public void markAsRead(Long notificationId, Long currentUserId, Authentication authentication) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại với ID: " + notificationId));

        if (!notification.getReceiver().getId().equals(currentUserId)) {
            throw new ForbiddenException("User not authorized to mark this notification as read");
        }

        // Use the exact setter name from your entity: setIsRead
        notification.setIsRead(true); // <--- CORRECT CALL based on your entity
        notification.setReadAt(LocalDateTime.now()); // You might also want to set the read time

        notificationRepository.save(notification);
    }

    // Xóa thông báo cũ
    public void deleteOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotifications(cutoffDate);
    }

    // Lấy thông báo theo loại
    public Page<NotificationDTO> getNotificationsByType(Long userId, Notification.NotificationType notificationType, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReceiverIdAndNotificationTypeOrderByCreatedAtDesc(userId, notificationType, pageable);
        return notifications.map(this::convertToDTO);
    }

    // Tạo thông báo khi có reaction
    public void createReactionNotification(Long postOwnerId, Long commentOwnerId, Long replyOwnerId,
                                           Long senderId, String senderUsername,
                                           Notification.NotificationType notificationType,
                                           String targetType, Long targetId) {

        if (senderId == null) return; // Không tạo thông báo cho hệ thống

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
                // Các loại thông báo khác sẽ được xử lý riêng
                break;
        }

        if (receiverId != null && !receiverId.equals(senderId)) {
            createNotification(receiverId, senderId, notificationType, targetType, targetId, title, content);
        }
    }

    // Tạo thông báo khi có comment
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

    // Convert entity to DTO
    private NotificationDTO convertToDTO(Notification notification) {
        String senderUsername = "Hệ thống";
        String senderAvatar = null;

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
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                actionUrl
        );
    }

    // Tạo URL điều hướng dựa trên loại thông báo
    private String generateActionUrl(Notification notification) {
        String baseUrl = "/"; // Có thể config từ application.properties

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
            default:
                return baseUrl;
        }
    }

    // Helper methods để lấy postId từ commentId hoặc replyId
    private Long getPostIdFromComment(Long commentId) {
        // Cần implement logic để lấy postId từ commentId
        // Có thể inject CommentRepository và query
        return null; // Placeholder
    }

    private Long getPostIdFromReply(Long replyId) {
        // Cần implement logic để lấy postId từ replyId
        // Có thể inject ReplyCommentRepository và query
        return null; // Placeholder
    }

    public boolean isReceiver(Authentication authentication, Long notificationId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) return false;

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy Notification: " + notificationId));

        // So sánh ID của người nhận trong thông báo với ID của người đang đăng nhập
        return notification.getReceiver().getId().equals(currentUser.getId());
    }
}
