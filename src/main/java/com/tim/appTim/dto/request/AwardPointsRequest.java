package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


public class AwardPointsRequest {
    private Long userId;
    private Integer behaviorId;

    public AwardPointsRequest() {}

    public AwardPointsRequest(Long userId, Integer behaviorId) {
        this.userId = userId;
        this.behaviorId = behaviorId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getBehaviorId() { return behaviorId; }
    public void setBehaviorId(Integer behaviorId) { this.behaviorId = behaviorId; }
}






