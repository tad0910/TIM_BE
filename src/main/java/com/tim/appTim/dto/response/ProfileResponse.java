package com.tim.appTim.dto.response;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import com.tim.appTim.entity.User;

public class ProfileResponse {
    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String profileImage;
    private String role;
    private LocalDateTime createdAt;
    private Page<PostDTO> posts;
    private List<UserImageDTO> images;
    private List<ProgramsDTO> programs;

    public ProfileResponse(User user, Page<PostDTO> posts, List<UserImageDTO> images, List<ProgramsDTO> programs) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phoneNumber = user.getPhoneNumber();
        this.profileImage = user.getProfileImage();
        this.role = user.getRoles().stream()
                .findFirst()
                .map(com.tim.appTim.entity.Role::getName)
                .orElse(null); 
        this.createdAt = user.getCreatedAt();
        this.posts = posts;
        this.images = images;
        this.programs = programs;
    }

    public ProfileResponse() {}
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
    public Page<PostDTO> getPosts() { return posts; }
    public void setPosts(Page<PostDTO> posts) { this.posts = posts; }
    public List<UserImageDTO> getImages() { return images; }
    public void setImages(List<UserImageDTO> images) { this.images = images; }
    public List<ProgramsDTO> getPrograms() { return programs; }
    public void setPrograms(List<ProgramsDTO> programs) { this.programs = programs; }

    @Deprecated
    public List<CourseDTO> getCourses() { 
        return List.of(); 
    }
    @Deprecated
    public void setCourses(List<CourseDTO> courses) { 

    }

}




