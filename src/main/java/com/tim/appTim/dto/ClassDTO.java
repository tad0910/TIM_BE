package com.tim.appTim.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ClassDTO {
    private String className;
    private String description;
    private List<MemberDTO> members;
    private ProgramsDTO program;

    public ClassDTO(String className, String description, List<MemberDTO> members) {
        this.className = className;
        this.description = description;
        this.members = members;
    }

    public ClassDTO(String className, String description, List<MemberDTO> members, ProgramsDTO program) {
        this.className = className;
        this.description = description;
        this.members = members;
        this.program = program;
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<MemberDTO> getMembers() { return members; }
    public void setMembers(List<MemberDTO> members) { this.members = members; }
    
    public ProgramsDTO getProgram() { return program; }
    public void setProgram(ProgramsDTO program) { this.program = program; }

    public static class MemberDTO {
        private Long userId;
        private String role;
        private LocalDateTime joinDate;

        public MemberDTO(Long userId, String role, LocalDateTime joinDate) {
            this.userId = userId;
            this.role = role;
            this.joinDate = joinDate;
        }

        public Long getUserId() { return userId; }
        public String getRole() { return role; }
        public LocalDateTime getJoinDate() { return joinDate; }
    }
}