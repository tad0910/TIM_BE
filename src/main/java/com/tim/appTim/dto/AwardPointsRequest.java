package com.tim.appTim.dto;

public class AwardPointsRequest {
    private Long userId;
    private String behaviorName;

    public AwardPointsRequest() {}

    public AwardPointsRequest(Long userId, String behaviorName) {
        this.userId = userId;
        this.behaviorName = behaviorName;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBehaviorName() { return behaviorName; }
    public void setBehaviorName(String behaviorName) { this.behaviorName = behaviorName; }
}

