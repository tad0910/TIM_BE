package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bai_viet_id", nullable = false)
    private Long postId;

    @Column(name = "nguoi_dung_id", nullable = false)
    private Long userId;

    @Column(name = "noi_dung")
    private String content;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime createdAt;

    @Column(name = "emotion")
    @Enumerated(EnumType.STRING)
    private Emotion emotion;

    @Column(name = "files_id")
    private Long fileId;

    @Column(name = "reaction_id")
    private Long reactionId;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<File> files = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "bai_viet_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "nguoi_dung_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    public enum Emotion {
        like, love, haha, sad, angry
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Emotion getEmotion() { return emotion; }
    public void setEmotion(Emotion emotion) { this.emotion = emotion; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public Long getReactionId() { return reactionId; }
    public void setReactionId(Long reactionId) { this.reactionId = reactionId; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<File> getFiles() { return files; }
    public void setFiles(List<File> files) { this.files = files; }
    public void addFile(File file) {
        files.add(file);
        file.setComment(this);
    }

    public void removeFile(File file) {
        files.remove(file);
        file.setComment(null);
    }

}