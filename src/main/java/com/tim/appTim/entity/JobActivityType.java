package com.tim.appTim.entity;

public enum JobActivityType {
    SEND_CV("Gửi CV"),
    INTERVIEW_SCHEDULED("Nhận lịch phỏng vấn"),
    INTERVIEW("Phỏng vấn"),
    OFFER_RECEIVED("Nhận offer"),
    PROBATION_CONTRACT("Ký hợp đồng thử việc"),
    OFFICIAL_CONTRACT("Ký hợp đồng chính thức");

    private final String displayName;

    JobActivityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
