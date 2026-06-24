package com.tim.appTim.service;

import com.tim.appTim.dto.request.JobLeadDTO;
import java.util.List;

public interface JobLeadService {
    JobLeadDTO create(Long studentId, JobLeadDTO dto);
    List<JobLeadDTO> getMyLeads(Long studentId);
    void delete(Long leadId, Long studentId);
}
