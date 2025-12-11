package com.tim.appTim.dto;

public class BehaviorPointTypeDTO {
    private Integer id;
    private Integer pointTypeId;
    private String pointTypeName;
    private Integer points;
    private Long notificationTemplateId;

    public BehaviorPointTypeDTO() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getPointTypeId() { return pointTypeId; }
    public void setPointTypeId(Integer pointTypeId) { this.pointTypeId = pointTypeId; }

    public String getPointTypeName() { return pointTypeName; }
    public void setPointTypeName(String pointTypeName) { this.pointTypeName = pointTypeName; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public Long getNotificationTemplateId() { return notificationTemplateId; }
    public void setNotificationTemplateId(Long notificationTemplateId) { this.notificationTemplateId = notificationTemplateId; }
}

