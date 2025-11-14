package com.tim.appTim.repository;// Thay đổi package cho đúng


import com.tim.appTim.dto.StudentGradeDTO;
import com.tim.appTim.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    // Kịch bản 1: Sinh viên xem điểm (dùng DTO projection)
    @Query("SELECT new com.tim.appTim.dto.StudentGradeDTO(g.componentName, g.score, g.maxScore, g.weightPercent, g.updatedAt) " +
            "FROM Grade g " +
            "WHERE g.classModule.id = :classModuleId AND g.student.id = :studentId")
    List<StudentGradeDTO> findGradesForStudent(Long classModuleId, Long studentId);

    // Kịch bản 2: Giảng viên xem điểm (dùng DTO projection)
    @Query("SELECT new com.tim.appTim.dto.StudentGradeDTO(g.id, g.componentName, g.score, g.maxScore, g.weightPercent, g.updatedAt) " +
            "FROM Grade g " +
            "WHERE g.classModule.id = :classModuleId AND g.student.id = :studentId")
    List<StudentGradeDTO> findGradesForTeacher(Long classModuleId, Long studentId);

    // Kịch bản 3: Lấy tất cả bản ghi điểm của lớp
    List<Grade> findByClassModuleId(Long classModuleId);

    // Kịch bản 3: Lấy tên các cột điểm
    @Query("SELECT DISTINCT g.componentName FROM Grade g WHERE g.classModule.id = :classModuleId")
    List<String> findDistinctComponentNamesByClassModuleId(Long classModuleId);
}