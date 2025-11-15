package com.tim.appTim.repository;

import com.tim.appTim.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    // Query cho lịch sử điểm danh (Task 1) - Điều chỉnh từ query SQL trước
    @Query(value = "SELECT cms.id AS schedule_id, m.name AS module_name, ms.session_number, ms.title AS session_title, "
            + "DATE(cms.start_date) AS session_date, ass.opened_at, ass.closed_at, ass.is_late, "
            + "CONCAT(opener.firstname, ' ', opener.lastname) AS opened_by_name, "
            + "CONCAT(marker.firstname, ' ', marker.lastname) AS marked_by_name "
            + "FROM class_module_schedules cms "
            + "JOIN class_module cm ON cms.class_module_id = cm.id "
            + "JOIN modules m ON cm.module_id = m.id "
            + "JOIN module_sessions ms ON cms.module_session_id = ms.id "
            + "LEFT JOIN attendance_sessions ass ON ass.schedule_id = cms.id "
            + "LEFT JOIN users opener ON ass.opened_by = opener.id "
            + "LEFT JOIN attendance_records ar ON ar.schedule_id = cms.id "
            + "LEFT JOIN users marker ON ar.marked_by = marker.id "
            + "WHERE cm.class_id = ?1 "
            + "GROUP BY cms.id, ass.opened_at, opener.firstname, opener.lastname "
            + "ORDER BY cms.start_date DESC", nativeQuery = true)
    List<Object[]> getAttendanceHistoryByClassId(Integer classId);
    Optional<AttendanceSession> findByScheduleId(Long scheduleId);
}