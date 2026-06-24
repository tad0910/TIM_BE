package com.tim.appTim.dto.request;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


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




