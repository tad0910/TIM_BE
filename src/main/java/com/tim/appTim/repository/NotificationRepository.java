package com.tim.appTim.repository;

import com.tim.appTim.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.transaction.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);

    List<Notification> findByReceiverIdAndNotificationTypeAndTargetTypeAndTargetId(
            Long receiverId,
            Notification.NotificationType notificationType,
            String targetType,
            Long targetId
    );

    boolean existsByReceiverIdAndSenderIdAndNotificationTypeAndTargetTypeAndTargetId(
            Long receiverId,
            Long senderId,
            Notification.NotificationType notificationType,
            String targetType,
            Long targetId
    );

    // ✅ Đánh dấu tất cả là đã đọc
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.receiverId = :receiverId AND n.isRead = false")
    void markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);

    // ✅ Đánh dấu một thông báo là đã đọc
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :notificationId AND n.receiverId = :receiverId")
    void markAsReadByIdAndReceiverId(@Param("notificationId") Long notificationId, @Param("receiverId") Long receiverId);

    // ✅ Xóa thông báo cũ
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    Page<Notification> findByReceiverIdAndNotificationTypeOrderByCreatedAtDesc(
            Long receiverId,
            Notification.NotificationType notificationType,
            Pageable pageable
    );
}