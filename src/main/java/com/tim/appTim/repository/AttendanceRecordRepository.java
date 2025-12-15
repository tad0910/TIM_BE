package com.tim.appTim.repository;

import com.tim.appTim.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {


    @Query(value = "SELECT u.id AS student_id, CONCAT(u.firstname, ' ', u.lastname) AS student_name, "
            + "ar.status, ar.marked_at, ar.notes, ar.marked_by "
            + "FROM class_members cmem "
            + "JOIN users u ON cmem.nguoi_dung_id = u.id AND cmem.vai_tro = 'sinh_vien' "
            + "LEFT JOIN attendance_records ar ON ar.schedule_id = ?1 AND ar.student_id = u.id "
            + "WHERE cmem.lop_id = (SELECT class_id FROM class_module_schedules WHERE id = ?1) "
            + "ORDER BY u.lastname, u.firstname", nativeQuery = true)
    List<Object[]> getAttendanceDetailsByScheduleId(Long scheduleId);

    @Query(value = "SELECT u.id AS student_id, CONCAT(u.firstname, ' ', u.lastname) AS student_name, "
            + "SUM(CASE WHEN ar.status IN ('present', 'late') THEN 1 ELSE 0 END) AS attended_count, "
            + "COUNT(cms.id) AS total_sessions, "
            + "ROUND(SUM(CASE WHEN ar.status IN ('present', 'late') THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(cms.id), 0), 2) AS attendance_rate "
            + "FROM class_members cmem "
            + "JOIN users u ON cmem.nguoi_dung_id = u.id AND cmem.vai_tro = 'sinh_vien' "
            + "JOIN class_module_schedules cms ON cms.class_id = cmem.lop_id "
            + "LEFT JOIN attendance_records ar ON ar.schedule_id = cms.id AND ar.student_id = u.id "
            + "WHERE cmem.lop_id = ?1 "
            + "GROUP BY u.id "
            + "ORDER BY attendance_rate DESC", nativeQuery = true)
    List<Object[]> getAttendanceStatsByClassId(Integer classId);
    Long countByScheduleId(Long scheduleId);
    Optional<AttendanceRecord> findByScheduleIdAndStudentId(Long scheduleId, Integer studentId);
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.scheduleId = ?1 AND ar.markedBy = ?2")
    Long countByScheduleIdAndMarkedBy(Long scheduleId, Integer markedBy);
}