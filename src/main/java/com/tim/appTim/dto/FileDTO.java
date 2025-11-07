package com.tim.appTim.dto;
public class FileDTO {
    private Integer id;
    private String fileUrl;
    private String fileType;
    private String fileName;
    private Long fileSize;

    public FileDTO() {}
    public FileDTO(Integer id, String fileUrl, String fileType, String fileName, Long fileSize) {
        this.id = id;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileSize = fileSize;
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
    public String getFileType() {
        return fileType;
    }
    public void setFileType(String fileType) {
        this.fileType = fileType;
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
}
