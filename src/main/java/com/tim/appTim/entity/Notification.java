package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId; // Người nhận thông báo

    @Column(name = "sender_id")
    private Long senderId; // Người gửi thông báo (có thể null cho thông báo hệ thống)

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "target_type", length = 50)
    private String targetType; // "POST", "COMMENT", "REPLY_COMMENT", "USER"

    @Column(name = "target_id")
    private Long targetId; // ID của đối tượng liên quan

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // Relations
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User sender;

    // Enum cho các loại thông báo
    public enum NotificationType {
        POST_REACTION,           // Ai đó like bài viết của bạn
        POST_COMMENT,        // Ai đó comment bài viết của bạn
        COMMENT_REACTION,        // Ai đó like comment của bạn
        COMMENT_REPLY,       // Ai đó reply comment của bạn
        REPLY_REACTION,          // Ai đó like reply của bạn
        USER_FOLLOW,         // Ai đó follow bạn
        POST_MENTION,        // Được mention trong bài viết
        COMMENT_MENTION,     // Được mention trong comment
        SYSTEM_ANNOUNCEMENT  // Thông báo hệ thống
    }

    // Constructors
    public Notification() {}

    public Notification(Long receiverId, Long senderId, NotificationType notificationType,
                        String targetType, Long targetId, String title, String content) {
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.notificationType = notificationType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.title = title;
        this.content = content;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
}
