package com.tim.appTim.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "files")
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

//    @Column(name = "post_id", nullable = false)
//    private Integer postId;

    @Column(name = "file_url", nullable = false, length = 255)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private FileType fileType;

    @ManyToOne // Thêm fetch type để tối ưu
    @JoinColumn(name = "post_id", nullable = false) // <-- SỬA Ở ĐÂY: Chỉ định cột khóa ngoại
    @JsonIgnore // Tránh lỗi đệ quy vô hạn khi serialize JSON
    private Post post;

    public enum FileType { IMAGE, VIDEO, DOCUMENT; }

    // Getters/Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
//    public Integer getPostId() { return postId; }
//    public void setPostId(Integer postId) { this.postId = postId; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
}
