package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.time.LocalDateTime;
import java.util.List;

public class ReplyCommentDTO {
    private Long id;
    private Long userId;
    private Long commentId;
    private String username;
    private String fullName;
    private String content;
    private String emotion;
    private Long fileId;
    private LocalDateTime createdAt;
    private String userAvatar;
    private List<FileDTO> files;

    public ReplyCommentDTO(Long id, Long commentId, Long userId, String username, String fullName, String content, String emotion, LocalDateTime createdAt, String userAvatar, List<FileDTO> files) {
        this.id = id;
        this.commentId = commentId;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.content = content;
        this.emotion = emotion ;
        this.createdAt = createdAt;
        this.userAvatar = userAvatar;
        this.files = files;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public List<FileDTO> getFiles() { return files; }
    public void setFiles(List<FileDTO> files) { this.files = files; }
}
