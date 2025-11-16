package com.tim.appTim.repository;


import com.tim.appTim.dto.StudentGradeDTO;
import com.tim.appTim.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    @Query("SELECT new com.tim.appTim.dto.StudentGradeDTO(g.componentName, g.score, g.maxScore, g.weightPercent, g.updatedAt) " +
            "FROM Grade g " +
            "WHERE g.classModule.id = :classModuleId AND g.student.id = :studentId")
    List<StudentGradeDTO> findGradesForStudent(Long classModuleId, Long studentId);

    @Query("SELECT new com.tim.appTim.dto.StudentGradeDTO(g.id, g.componentName, g.score, g.maxScore, g.weightPercent, g.updatedAt) " +
            "FROM Grade g " +
            "WHERE g.classModule.id = :classModuleId AND g.student.id = :studentId")
    List<StudentGradeDTO> findGradesForTeacher(Long classModuleId, Long studentId);

    List<Grade> findByClassModuleId(Long classModuleId);

    @Query("SELECT DISTINCT g.componentName FROM Grade g WHERE g.classModule.id = :classModuleId")
    List<String> findDistinctComponentNamesByClassModuleId(Long classModuleId);

    List<Grade> findByClassModuleIdAndStudentIdIn(Long classModuleId, List<Long> studentIds);

}