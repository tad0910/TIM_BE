package com.tim.appTim.dto;

import com.tim.appTim.entity.StudentForm;


public class ApprovalRequestDTO {
    private Long formId;
    private StudentForm.ApprovalStatus decision; 
    private String note;
    private String targetRole;
    
    public Long getFormId() {return formId;    }
    public void setFormId(Long formId) {this.formId = formId;    }

    public StudentForm.ApprovalStatus getDecision() {return decision;    }
    public void setDecision(StudentForm.ApprovalStatus decision) {this.decision = decision;    }

    public String getNote() {return note;    }
    public void setNote(String note) {this.note = note;    }

    public String getTargetRole() {return targetRole;    }
    public void setTargetRole(String targetRole) {this.targetRole = targetRole;    }
    
}