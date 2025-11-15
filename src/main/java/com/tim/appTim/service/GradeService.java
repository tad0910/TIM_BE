package com.tim.appTim.service;// Thay đổi package cho đúng


import com.tim.appTim.dto.GradeHistoryDTO;
import com.tim.appTim.dto.GradeUpdateDTO;
import com.tim.appTim.dto.GradebookDTO;
import com.tim.appTim.dto.StudentGradeDTO;
import com.tim.appTim.entity.User;

import java.util.List;

public interface GradeService {

    List<StudentGradeDTO> getMyGrades(Long classModuleId, Long studentId);

    List<StudentGradeDTO> getStudentGrades(Long classModuleId, Long studentId);

    GradebookDTO getGradebook(Long classModuleId, Long teacherId);

    void validateTeacherPermission(Long classModuleId, Long teacherId);

    StudentGradeDTO updateGrade(Long gradeId, GradeUpdateDTO dto, User teacher);

    List<GradeHistoryDTO> getGradeHistory(Long gradeId, User currentUser);
}
