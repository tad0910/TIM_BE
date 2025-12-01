package com.tim.appTim.dto;

/**
 * DTO cho thông tin file hướng dẫn Gamification
 * Sử dụng File entity, nên chỉ cần các trường cơ bản
 */
public class GamificationGuideDTO {
    private Integer id;
    private String fileUrl;
    private String fileName; // Tên file gốc (đã bỏ prefix GAMIFICATION_GUIDE_)
    private Long fileSize;
    private String fileType; // DOCUMENT, IMAGE, VIDEO

    // Constructors
    public GamificationGuideDTO() {
    }

    public GamificationGuideDTO(Integer id, String fileUrl, String fileName, Long fileSize, String fileType) {
        this.id = id;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}

