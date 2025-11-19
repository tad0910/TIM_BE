package com.tim.appTim.service;


import com.tim.appTim.dto.*;
import com.tim.appTim.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GradeService {

    void batchCreateOrUpdateGrades(BatchGradeUpdateDTO dto, User teacher);

    /**
     * API CŨ (ĐÃ SỬA): Lấy sổ điểm (có phân trang)
     */
    GradebookDTO getGradebook(Long classModuleId, Long teacherId, Pageable pageable);

    /**
     * API CŨ (ĐÃ SỬA): Lấy điểm của 1 sinh viên (dùng cho SV xem điểm)
     */
    GradeDTO getMyGrades(Long classModuleId, Long studentId); // Sửa: Trả về GradeDTO mới

    /**
     * Lấy lịch sử thay đổi của 1 hàng điểm
     */
    List<GradeHistoryDTO> getGradeHistory(Long gradeId, User currentUser);

    // --- CÁC HÀM HELPER ---
    void validateTeacherPermission(Long classModuleId, Long teacherId);

    void deleteGrade(Long gradeId, User currentUser);
}
