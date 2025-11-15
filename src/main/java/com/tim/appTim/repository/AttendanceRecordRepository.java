package com.tim.appTim.repository;

import com.tim.appTim.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    // Query cho thống kê tỷ lệ điểm danh (Task 3) - Theo lớp
    @Query(value = "SELECT u.id AS student_id, CONCAT(u.firstname, ' ', u.lastname) AS student_name, "
            + "COUNT(IF(ar.status IN ('present', 'late'), 1, NULL)) AS attended_count, "
            + "COUNT(cms.id) AS total_sessions, "
            + "ROUND(COUNT(IF(ar.status IN ('present', 'late'), 1, NULL)) * 100.0 / COUNT(cms.id), 2) AS attendance_rate "
            + "FROM class_members cmem "
            + "JOIN users u ON cmem.nguoi_dung_id = u.id AND cmem.vai_tro = 'sinh_vien' "
            + "JOIN class_module cm ON cm.class_id = cmem.lop_id "
            + "JOIN class_module_schedules cms ON cms.class_module_id = cm.id "
            + "LEFT JOIN attendance_records ar ON ar.schedule_id = cms.id AND ar.student_id = u.id "
            + "WHERE cmem.lop_id = ?1 "
            + "GROUP BY u.id "
            + "ORDER BY attendance_rate DESC", nativeQuery = true)
    List<Object[]> getAttendanceStatsByClassId(Integer classId);
    Optional<AttendanceRecord> findByScheduleIdAndStudentId(Long scheduleId, Integer studentId);
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.scheduleId = ?1 AND ar.markedBy = ?2")
    Long countByScheduleIdAndMarkedBy(Long scheduleId, Integer markedBy);
}