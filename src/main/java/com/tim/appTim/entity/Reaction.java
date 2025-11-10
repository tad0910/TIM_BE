package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reactions")
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loai_cam_xuc")
    @Enumerated(EnumType.STRING)
    private EmotionType emotionType;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bai_viet_id") 
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id") 
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_comment_id") 
    private ReplyComment replyComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false) 
    private User user;

    public enum EmotionType {
        like, love, haha, wow, sad, angry
    }


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

}