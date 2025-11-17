package com.tim.appTim.dto;

import java.util.List;

public class AddMembersBatchRequest {
    private List<AddMemberRequest> members;

    public List<AddMemberRequest> getMembers() {
        return members;
    }

    public void setMembers(List<AddMemberRequest> members) {
        this.members = members;
    }
}