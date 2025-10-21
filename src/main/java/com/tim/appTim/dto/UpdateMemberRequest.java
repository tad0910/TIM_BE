package com.tim.appTim.dto;

import com.tim.appTim.entity.ClassMember.Role;

public class UpdateMemberRequest {
    private Role role;

    public UpdateMemberRequest() {}

    public UpdateMemberRequest(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
