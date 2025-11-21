package com.tim.appTim.service;


import com.tim.appTim.dto.*;
import com.tim.appTim.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GradeService {

    void batchCreateOrUpdateGrades(BatchGradeUpdateDTO dto, User teacher);

    GradebookDTO getGradebook(Long classModuleId, Long teacherId, Pageable pageable);

    GradeDTO getMyGrades(Long classModuleId, Long studentId);

    List<GradeHistoryDTO> getGradeHistory(Long gradeId, User currentUser);

    void validateTeacherPermission(Long classModuleId, Long teacherId);

    void deleteGrade(Long gradeId, User currentUser);
}
