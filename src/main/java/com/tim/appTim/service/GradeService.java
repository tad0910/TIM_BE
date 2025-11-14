package com.tim.appTim.service;// Thay đổi package cho đúng


import com.tim.appTim.dto.GradebookDTO;
import com.tim.appTim.dto.StudentGradeDTO;

import java.util.List;

public interface GradeService {

    List<StudentGradeDTO> getMyGrades(Long classModuleId, Long studentId);

    // Kịch bản 2 (thay Integer studentId -> Long studentId)
    List<StudentGradeDTO> getStudentGrades(Long classModuleId, Long studentId);

    // Kịch bản 3 (thay Integer teacherId -> Long teacherId)
    GradebookDTO getGradebook(Long classModuleId, Long teacherId);

    // Hàm này được gọi từ Controller (để tái sử dụng)
    void validateTeacherPermission(Long classModuleId, Long teacherId);
}