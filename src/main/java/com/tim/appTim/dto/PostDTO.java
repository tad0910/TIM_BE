package com.tim.appTim.dto;
import java.time.LocalDateTime;
import java.util.List;

public class PostDTO {
    private Long id;
    private Long userId;
    private String content;
    private String privacy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalReactions;
    private Integer totalComments;
    private List<CommentDTO> comments;
    private List<ReactionDTO> reactions;
    private List<FileDTO> files;
    private String userAvatar;
    private String username;
    private String fullName;
    private LinkPreviewDTO linkPreview;
      
    public PostDTO() {
    }

    public PostDTO(Long id,Long userId, String content, String privacy, LocalDateTime createdAt, LocalDateTime updatedAt,
                   Integer totalReactions, Integer totalComments,
                   List<CommentDTO> comments, List<ReactionDTO> reactions, List<FileDTO> files, String userAvatar, String username, String fullName,
                   LinkPreviewDTO linkPreview) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.privacy = privacy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.totalReactions = totalReactions;
        this.totalComments = totalComments;
        this.comments = comments;
        this.reactions = reactions;
        this.files = files;
        this.userAvatar = userAvatar;
        this.username = username;
        this.fullName = fullName;
        this.linkPreview = linkPreview;
    }


    public PostDTO(Long id, Long userId, String content, String privacy, LocalDateTime createdAt, LocalDateTime updatedAt,
                   List<CommentDTO> comments, List<ReactionDTO> reactions, List<com.tim.appTim.dto.FileDTO> files) {
        this(id, userId, content, privacy, createdAt, updatedAt,
                reactions != null ? reactions.size() : 0,
                comments != null ? comments.size() : 0,
                comments, reactions, files, null, null, null, null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }
    public List<ReactionDTO> getReactions() { return reactions; }
    public void setReactions(List<ReactionDTO> reactions) { this.reactions = reactions; }
    public List<FileDTO> getFiles() { return files; }
    public void setFiles(List<FileDTO> files) { this.files = files; }
    public Integer getTotalReactions() { return totalReactions; }
    public void setTotalReactions(Integer totalReactions) { this.totalReactions = totalReactions; }
    public Integer getTotalComments() { return totalComments; }
    public void setTotalComments(Integer totalComments) { this.totalComments = totalComments; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LinkPreviewDTO getLinkPreview() { return linkPreview; }
    public void setLinkPreview(LinkPreviewDTO linkPreview) { this.linkPreview = linkPreview; }
}
