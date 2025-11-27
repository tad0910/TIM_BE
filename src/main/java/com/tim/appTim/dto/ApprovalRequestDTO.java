package com.tim.appTim.dto;

import com.tim.appTim.entity.StudentForm;


public class ApprovalRequestDTO {
    private Long formId;
    private StudentForm.ApprovalStatus decision; 
    private String note;
    private String targetRole;
    private Long moduleId;
    private Long moduleSessionId;
    
    public Long getFormId() {return formId;    }
    public void setFormId(Long formId) {this.formId = formId;    }

    public StudentForm.ApprovalStatus getDecision() {return decision;    }
    public void setDecision(StudentForm.ApprovalStatus decision) {this.decision = decision;    }

    public String getNote() {return note;    }
    public void setNote(String note) {this.note = note;    }

    public String getTargetRole() {return targetRole;    }
    public void setTargetRole(String targetRole) {this.targetRole = targetRole;    }
    
    public Long getModuleId() {return moduleId;    }
    public void setModuleId(Long moduleId) {this.moduleId = moduleId;    }

    public Long getModuleSessionId() {return moduleSessionId;    }
    public void setModuleSessionId(Long moduleSessionId) {this.moduleSessionId = moduleSessionId;    }
}