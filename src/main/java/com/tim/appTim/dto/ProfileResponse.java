package com.tim.appTim.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.tim.appTim.entity.User;

public class ProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String profileImage;
    private String role;
    private LocalDateTime createdAt;
    private List<PostDTO> posts;
    private List<UserImageDTO> images;
    private List<CourseDTO> courses;

    public ProfileResponse(User user, List<PostDTO> posts, List<UserImageDTO> images, List<CourseDTO> courses) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.profileImage = user.getProfileImage();
        this.role = user.getRole() != null ? user.getRole().name() : null;
        this.createdAt = user.getCreatedAt();
        this.posts = posts;
        this.images = images;
        this.courses = courses;
    }

    public ProfileResponse() {}

    // Getters/Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<PostDTO> getPosts() { return posts; }
    public void setPosts(List<PostDTO> posts) { this.posts = posts; }
    public List<UserImageDTO> getImages() { return images; }
    public void setImages(List<UserImageDTO> images) { this.images = images; }
    public List<CourseDTO> getCourses() { return courses; }
    public void setCourses(List<CourseDTO> courses) { this.courses = courses; }
}