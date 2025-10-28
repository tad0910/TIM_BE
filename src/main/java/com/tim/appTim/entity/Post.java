package com.tim.appTim.entity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;

import jakarta.persistence.*;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "noi_dung")
    private String content;

    @Column(name = "quyen_rieng_tu")
    @Enumerated(EnumType.STRING)
    private Privacy privacy;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime createdAt;

    @Column(name = "thoi_gian_cap_nhat")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", referencedColumnName = "id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<File> files = new ArrayList<>();

    @Column(name = "tong_reactions")
    private Integer totalReactions;

    @Column(name = "tong_comments")
    private Integer totalComments;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reaction> reactions = new ArrayList<>();
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "link_title", length = 500)
    private String linkTitle;

    @Column(name = "link_description", length = 1000)
    private String linkDescription;

    @Column(name = "link_image_url", length = 500)
    private String linkImageUrl;

    @Column(name = "link_domain", length = 200)
    private String linkDomain;

    public enum Privacy {
        open, friends, only_me
    }

    public void addFile(File file) {
        files.add(file);
        file.setPost(this);
    }

    public void removeFile(File file) {
        files.remove(file);
        file.setPost(null);
    }

    public boolean hasLinkPreview() {
        return linkUrl != null && linkTitle != null && !linkTitle.isEmpty();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Privacy getPrivacy() { return privacy; }
    public void setPrivacy(Privacy privacy) { this.privacy = privacy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<File> getFiles() { return files; }
    public void setFiles(List<File> files) { this.files = files; }
    public Integer getTotalReactions() { return totalReactions; }
    public void setTotalReactions(Integer totalReactions) { this.totalReactions = totalReactions; }
    public Integer getTotalComments() { return totalComments; }
    public void setTotalComments(Integer totalComments) { this.totalComments = totalComments; }
    public List<Comment> getComments() {
        return comments;
    }
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
    public List<Reaction> getReactions() {
        return reactions;
    }
    public void setReactions (List<Reaction> reactions) {
        this.reactions = reactions;
    }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public String getLinkTitle() { return linkTitle; }
    public void setLinkTitle(String linkTitle) { this.linkTitle = linkTitle; }
    public String getLinkDescription() { return linkDescription; }
    public void setLinkDescription(String linkDescription) { this.linkDescription = linkDescription; }
    public String getLinkImageUrl() { return linkImageUrl; }
    public void setLinkImageUrl(String linkImageUrl) { this.linkImageUrl = linkImageUrl; }
    public String getLinkDomain() { return linkDomain; }
    public void setLinkDomain(String linkDomain) { this.linkDomain = linkDomain; }

}