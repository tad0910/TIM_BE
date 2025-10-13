package com.tim.appTim.dto;

public class ReactionRequest {
    private Long userId;
    private String emotionType;

    // Getters/Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmotionType() { return emotionType; }
    public void setEmotionType(String emotionType) { this.emotionType = emotionType; }
    
}
