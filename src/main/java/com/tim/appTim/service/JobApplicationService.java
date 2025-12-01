package com.tim.appTim.service;

import com.tim.appTim.dto.StudentJobTrackingDTO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface JobApplicationService {
    void introduceBatch(Long classId, Long companyId, List<Long> studentIds, List<MultipartFile> cvFiles);

    List<StudentJobTrackingDTO> getJobTrackingList(Long classId);
}