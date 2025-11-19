package com.tim.appTim.repository;

import com.tim.appTim.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

        @Query(value = """
        SELECT 
            cms.id AS schedule_id,
            m.name AS module_name,
            ms.session_number,
            ms.title AS session_title,
            CAST(cms.start_date AS DATETIME) AS session_datetime,  
            ass.opened_at,
            ass.closed_at,
            ass.is_late,
            CONCAT(opener.firstname, ' ', opener.lastname) AS opened_by_name,
            MAX(CONCAT(marker.firstname, ' ', marker.lastname)) AS marked_by_name
        FROM class_module_schedules cms
        JOIN modules m ON cms.module_id = m.id
        JOIN module_sessions ms ON cms.module_session_id = ms.id
        LEFT JOIN attendance_sessions ass ON ass.schedule_id = cms.id
        LEFT JOIN users opener ON ass.opened_by = opener.id
        LEFT JOIN attendance_records ar ON ar.schedule_id = cms.id
        LEFT JOIN users marker ON ar.marked_by = marker.id
        WHERE cms.class_id = ?1
        GROUP BY 
            cms.id, 
            ass.opened_at, 
            ass.closed_at, 
            ass.is_late,
            opener.firstname, 
            opener.lastname,
            m.name,
            ms.session_number,
            ms.title,
            cms.start_date
        ORDER BY cms.start_date DESC
        """, nativeQuery = true)
    List<Object[]> getAttendanceHistoryByClassId(Integer classId);
    Optional<AttendanceSession> findByScheduleId(Long scheduleId);
    @Query(value = "SELECT COUNT(*) > 0 FROM class_module_schedules WHERE id = :scheduleId AND instructor_id = :teacherId", nativeQuery = true)
    boolean isAssignedInstructor(@Param("scheduleId") Long scheduleId, @Param("teacherId") Integer teacherId);
}