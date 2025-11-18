package com.tim.appTim.repository;

import com.tim.appTim.entity.Grade; // (Entity MỚI của bạn)
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    /**
     * HÀM MỚI (QUAN TRỌNG): Dùng cho logic "Upsert"
     * Tìm một hàng điểm duy nhất bằng studentId và classModuleId
     */
    Optional<Grade> findByStudentIdAndClassModuleIdAndStatus(Long studentId, Long classModuleId, Grade.Status status);

    /**
     * HÀM MỚI: Lấy tất cả điểm của các sinh viên trong một danh sách
     * (Dùng cho API Gradebook có phân trang)
     */
    List<Grade> findByClassModuleIdAndStudentIdInAndStatus(
            Long classModuleId, List<Long> studentIds, Grade.Status status);

    /**
     * HÀM CŨ (ĐÃ SỬA): Lấy điểm của sinh viên
     * (Chúng ta không thể dùng DTO projection cũ nữa vì cấu trúc đã thay đổi)
     */
    Optional<Grade> findByClassModuleIdAndStudentIdAndStatus(
            Long classModuleId, Long studentId, Grade.Status status);
}