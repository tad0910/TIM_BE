package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reactions")
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- XÓA CÁC TRƯỜNG ID THỦ CÔNG ---
    // @Column(name = "bai_viet_id")
    // private Long postId;
    // @Column(name = "comment_id")
    // private Long commentId;
    // @Column(name = "reply_comment_id")
    // private Long replyCommentId;
    // @Column(name = "nguoi_dung_id")
    // private Long userId;

    @Column(name = "loai_cam_xuc")
    @Enumerated(EnumType.STRING)
    private EmotionType emotionType;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime createdAt;

    // --- SỬA LẠI CÁC MAPPING (XÓA insertable=false, updatable=false) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bai_viet_id") // 'nullable = true' (vì reaction có thể cho comment)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id") // 'nullable = true'
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_comment_id") // 'nullable = true'
    private ReplyComment replyComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false) // User thì không được null
    private User user;

    // Giữ nguyên Enum của bạn
    public enum EmotionType {
        like, love, haha, wow, sad, angry
    }

    // --- CẬP NHẬT GETTERS/SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmotionType getEmotionType() { return emotionType; }
    public void setEmotionType(EmotionType emotionType) { this.emotionType = emotionType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }

    public ReplyComment getReplyComment() { return replyComment; }
    public void setReplyComment(ReplyComment replyComment) { this.replyComment = replyComment; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // --- XÓA CÁC GETTER/SETTER CŨ ---
    // public Long getPostId() { return postId; }
    // public void setPostId(Long postId) { this.postId = postId; }
    // public Long getUserId() { return userId; }
    // public void setUserId(Long userId) { this.userId = userId; }
    // public Long getCommentId() { return commentId; }
    // public void setCommentId(Long commentId) { this.commentId = commentId; }
    // public Long getReplyCommentId() { return replyCommentId; }
    // public void setReplyCommentId(Long replyCommentId) { this.replyCommentId = replyCommentId; }
}