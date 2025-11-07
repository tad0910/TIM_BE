package com.tim.appTim.repository;

import com.tim.appTim.entity.ClassModuleSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassModuleScheduleRepository extends JpaRepository<ClassModuleSchedule, Long> {

    List<ClassModuleSchedule> findByClassIdAndStartDateBetween(
            Long classId,
            LocalDate startDate,
            LocalDate endDate
    );


    @Query("SELECT s FROM ClassModuleSchedule s " +
            "WHERE s.instructorId = :instructorId " +
            "AND s.id != :scheduleId " +
            "AND (" +
            "   (s.startDate <= :newEndDate AND s.endDate >= :newStartDate)" +
            ")")
    List<ClassModuleSchedule> findConflictingSchedules(
            @Param("instructorId") Long instructorId,
            @Param("newStartDate") LocalDate newStartDate,
            @Param("newEndDate") LocalDate newEndDate,
            @Param("scheduleId") Long scheduleId
    );

    default List<ClassModuleSchedule> findConflictingSchedules(
            Long instructorId,
            LocalDate newStartDate,
            LocalDate newEndDate
    ) {
        return findConflictingSchedules(instructorId, newStartDate, newEndDate, 0L);
    }

    boolean existsByClassIdAndModuleId(Long classId, Long moduleId);

    List<ClassModuleSchedule> findByClassId(Long classId);

    List<ClassModuleSchedule> findByInstructorId(Long instructorId);

    Optional<ClassModuleSchedule> findByClassIdAndModuleId(Long classId, Long moduleId);

    List<ClassModuleSchedule> findByInstructorIdAndStartDateBetween(
            Long instructorId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate
    );

    List<ClassModuleSchedule> findByClassModuleId(Long classModuleId);

}