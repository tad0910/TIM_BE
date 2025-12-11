package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "behavior_point_types")
public class BehaviorPointType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "behavior_id", nullable = false)
    private GamificationBehavior behavior;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_type_id", nullable = false)
    private GamificationPointType pointType;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_template_id")
    private NotificationTemplate notificationTemplate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public GamificationBehavior getBehavior() { return behavior; }
    public void setBehavior(GamificationBehavior behavior) { this.behavior = behavior; }

    public GamificationPointType getPointType() { return pointType; }
    public void setPointType(GamificationPointType pointType) { this.pointType = pointType; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public NotificationTemplate getNotificationTemplate() { return notificationTemplate; }
    public void setNotificationTemplate(NotificationTemplate notificationTemplate) { this.notificationTemplate = notificationTemplate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

