package com.tim.appTim.dto;
import java.time.LocalDateTime;
import java.util.List;
import com.tim.appTim.entity.File;


public class PostDTO {
    private Long id;
    private Long userId;
    private String content;
    private String privacy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentDTO> comments;
    private List<ReactionDTO> reactions;
    private List<File> files;


    public PostDTO(Long id,Long userId, String content, String privacy, LocalDateTime createdAt, LocalDateTime updatedAt,
                   List<CommentDTO> comments, List<ReactionDTO> reactions, List<File> files ) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.privacy = privacy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.comments = comments;
        this.reactions = reactions;
        this.files = files;
    }
    // Getters/Setters
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
    public List<File> getFiles() { return files; }
    public void setFiles(List<File> files) { this.files = files; }
}