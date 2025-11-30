package com.tim.appTim.dto;

public class AwardPointsRequest {
    private Long userId;
    private String behaviorCode;

    public AwardPointsRequest() {}

    public AwardPointsRequest(Long userId, String behaviorCode) {
        this.userId = userId;
        this.behaviorCode = behaviorCode;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBehaviorCode() { return behaviorCode; }
    public void setBehaviorCode(String behaviorCode) { this.behaviorCode = behaviorCode; }
}

