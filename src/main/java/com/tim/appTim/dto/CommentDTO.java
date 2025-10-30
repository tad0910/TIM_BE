package com.tim.appTim.dto;
import java.time.LocalDateTime;
import java.util.List;

public class CommentDTO {
    private Long id;
    private Long userId;
    private String username;
    private String content;
    private String emotion;
    private Long fileId;
    private String userAvatar ;
    private LocalDateTime createdAt;
    private List<ReplyCommentDTO> replyComments;
    private List<FileDTO> files;

    public CommentDTO(Long id, Long userId, String username, String content,String userAvatar, String emotion,
                      LocalDateTime createdAt, List<ReplyCommentDTO> replyComments, List<FileDTO> files) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.userAvatar = userAvatar;
        this.emotion = emotion;
        this.createdAt = createdAt;
        this.replyComments = replyComments;
        this.files = files;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<ReplyCommentDTO> getReplyComments() { return replyComments; }
    public void setReplyComments(List<ReplyCommentDTO> replyComments) { this.replyComments = replyComments; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public List<FileDTO> getFiles() { return files; }
    public void setFiles(List<FileDTO> files) { this.files = files; }
}
