package com.tim.appTim.service;


import com.tim.appTim.dto.*;
import com.tim.appTim.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GradeService {

    List<StudentGradeDTO> getMyGrades(Long classModuleId, Long studentId);

    List<StudentGradeDTO> getStudentGrades(Long classModuleId, Long studentId);

    GradebookDTO getGradebook(Long classModuleId, Long teacherId, Pageable pageable);

    void validateTeacherPermission(Long classModuleId, Long teacherId);

    StudentGradeDTO updateGrade(Long gradeId, GradeUpdateDTO dto, User teacher);

    List<GradeHistoryDTO> getGradeHistory(Long gradeId, User currentUser);

    StudentGradeDTO createGrade(GradeCreateDTO dto, User teacher);
}
