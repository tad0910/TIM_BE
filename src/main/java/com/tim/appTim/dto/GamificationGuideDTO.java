package com.tim.appTim.dto;


public class GamificationGuideDTO {
    private Integer id;
    private String fileUrl;
    private String fileName; 
    private Long fileSize;
    private String fileType; 

    public GamificationGuideDTO() {
    }

    public GamificationGuideDTO(Integer id, String fileUrl, String fileName, Long fileSize, String fileType) {
        this.id = id;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
    }

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

