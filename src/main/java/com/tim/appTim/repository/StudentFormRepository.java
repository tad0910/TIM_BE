package com.tim.appTim.repository;

import com.tim.appTim.entity.StudentForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentFormRepository extends JpaRepository<StudentForm, Long> {
    List<StudentForm> findByStudentId(Long studentId);
    List<StudentForm> findByAcademicApproval(StudentForm.ApprovalStatus status);
    List<StudentForm> findByClassRoomIdIn(List<Long> classIds);

    long countByStatus(StudentForm.FormStatus status);

    List<StudentForm> findTop5ByStatusOrderByCreatedAtDesc(StudentForm.FormStatus status);
}