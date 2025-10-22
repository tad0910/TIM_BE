package com.tim.appTim.controller;

import com.tim.appTim.dto.NotificationDTO;
import com.tim.appTim.entity.Notification;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.NotificationService;
import com.tim.appTim.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@userService.isSelf(authentication, #userId)")
    public ResponseEntity<Page<NotificationDTO>> getNotificationsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationDTO> notifications = notificationService.getNotificationsByUserId(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("@userService.isSelf(authentication, #userId)")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(@PathVariable Long userId, Authentication authentication) {
        List<NotificationDTO> notifications = notificationService.getUnreadNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/unread-count")
    @PreAuthorize("@userService.isSelf(authentication, #userId)")
    public ResponseEntity<Long> getUnreadNotificationCount(@PathVariable Long userId, Authentication authentication) {
        long count = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/user/{userId}/mark-all-read")
    @PreAuthorize("@userService.isSelf(authentication, #userId)")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok("All notifications marked as read");
    }

    @PutMapping("/{notificationId}/mark-read")
    @PreAuthorize("@notificationService.isReceiver(authentication, #notificationId)")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        User currentUser = getUserFromAuthentication(authentication);
        notificationService.markAsRead(notificationId, currentUser.getId());
        return ResponseEntity.ok("Notification marked as read");
    }

    @GetMapping("/user/{userId}/type/{notificationType}")
    @PreAuthorize("@userService.isSelf(authentication, #userId)")
    public ResponseEntity<Page<NotificationDTO>> getNotificationsByType(
            @PathVariable Long userId,
            @PathVariable String notificationType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        try {
            Notification.NotificationType type = Notification.NotificationType.valueOf(notificationType.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationDTO> notifications = notificationService.getNotificationsByType(userId, type, pageable);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/cleanup")
    @PreAuthorize("hasAuthority('notification:cleanup')")
    public ResponseEntity<String> cleanupOldNotifications() {
        notificationService.deleteOldNotifications();
        return ResponseEntity.ok("Old notifications cleaned up");
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('notification:create_manual')")
    public ResponseEntity<NotificationDTO> createNotification(
            @RequestParam Long receiverId,
            @RequestParam(required = false) Long senderId,
            @RequestParam String notificationType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam String title,
            @RequestParam String content) {

        try {
            Notification.NotificationType type = Notification.NotificationType.valueOf(notificationType.toUpperCase());
            NotificationDTO notification = notificationService.createNotification(
                    receiverId, senderId, type, targetType, targetId, title, content);

            if (notification != null) {
                return ResponseEntity.ok(notification);
            } else {
                return ResponseEntity.badRequest().body(null);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
