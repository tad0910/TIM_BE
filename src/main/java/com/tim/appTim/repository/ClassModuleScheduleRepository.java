package com.tim.appTim.repository;

import com.tim.appTim.entity.ClassModuleSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassModuleScheduleRepository extends JpaRepository<ClassModuleSchedule, Long> {

    List<ClassModuleSchedule> findByClassIdAndStartDateBetween(
            Long classId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT s FROM ClassModuleSchedule s " +
            "WHERE s.instructorId = :instructorId " +
            "AND s.id != :scheduleId " +
            "AND (" +
            "   (s.startDate <= :newEndDate AND s.endDate >= :newStartDate)" +
            ")")
    List<ClassModuleSchedule> findConflictingSchedules(
            @Param("instructorId") Long instructorId,
            @Param("newStartDate") LocalDateTime newStartDate,
            @Param("newEndDate") LocalDateTime newEndDate,
            @Param("scheduleId") Long scheduleId
    );

    default List<ClassModuleSchedule> findConflictingSchedules(
            Long instructorId,
            LocalDateTime newStartDate,
            LocalDateTime newEndDate
    ) {
        return findConflictingSchedules(instructorId, newStartDate, newEndDate, 0L);  
    }

    boolean existsByClassIdAndModuleId(Long classId, Integer moduleId);

    List<ClassModuleSchedule> findByClassId(Long classId);

    List<ClassModuleSchedule> findByInstructorId(Long instructorId);

    Optional<ClassModuleSchedule> findByClassIdAndModuleId(Long classId, Integer moduleId);

    List<ClassModuleSchedule> findByInstructorIdAndStartDateBetween(
            Long instructorId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<ClassModuleSchedule> findByClassModuleId(Long classModuleId);

}