package com.tim.appTim.dto;

import com.tim.appTim.entity.User;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String profileImage;
    private String role;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.profileImage = user.getProfileImage();
        this.role = user.getRole().name(); // enum -> String
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getProfileImage() { return profileImage; }
    public String getRole() { return role; }
}
