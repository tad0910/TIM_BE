package com.tim.appTim.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ClassDTO {
    private Long id;
    private String className;
    private String description;
    private List<MemberDTO> members;
    private Integer programId;
    private ProgramsDTO program;
    private boolean jobsEnabled;
    
    public ClassDTO() {
    }

    public ClassDTO(Long id, String className, String description, List<MemberDTO> members, Integer programId, ProgramsDTO program) {
        this(id, className, description, members, programId, program, false);
    }

    public ClassDTO(Long id, String className, String description, List<MemberDTO> members, Integer programId, ProgramsDTO program, boolean jobsEnabled) {
        this.id = id;
        this.className = className;
        this.description = description;
        this.members = members;
        this.programId = programId;
        this.program = program;
        this.jobsEnabled = jobsEnabled;
    }


    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<MemberDTO> getMembers() { return members; }
    public void setMembers(List<MemberDTO> members) { this.members = members; }
    
    public Integer getProgramId() {return programId;}
    public void setProgramId(Integer programId) {this.programId = programId;}
    
    public ProgramsDTO getProgram() { return program; }
    public void setProgram(ProgramsDTO program) { this.program = program; }

    public boolean isJobsEnabled() { return jobsEnabled; }
    public void setJobsEnabled(boolean jobsEnabled) { this.jobsEnabled = jobsEnabled; }

    public static class MemberDTO {
        private Long userId;
        private String role;
        private LocalDateTime joinDate;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String profileImage;

        public MemberDTO() {
        }

        public MemberDTO(Long userId, String role, LocalDateTime joinDate) {
            this.userId = userId;
            this.role = role;
            this.joinDate = joinDate;
        }

        public MemberDTO(Long userId, String role, LocalDateTime joinDate, String username, String firstName, String lastName, String email, String profileImage) {
            this.userId = userId;
            this.role = role;
            this.joinDate = joinDate;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.profileImage = profileImage;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public LocalDateTime getJoinDate() { return joinDate; }
        public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getProfileImage() { return profileImage; }
        public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    }
}