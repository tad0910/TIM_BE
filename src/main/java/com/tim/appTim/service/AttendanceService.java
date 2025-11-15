package com.tim.appTim.service;

import com.tim.appTim.dto.MarkAttendanceRequest;
import com.tim.appTim.dto.AttendanceHistoryDto;
import com.tim.appTim.dto.AttendanceMarkDto;
import com.tim.appTim.dto.AttendanceStatsDto;
import com.tim.appTim.entity.AttendanceSession;
import com.tim.appTim.entity.AttendanceRecord;
import com.tim.appTim.repository.AttendanceSessionRepository;
import com.tim.appTim.repository.AttendanceRecordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             AttendanceRecordRepository recordRepository) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
    }

    public List<AttendanceHistoryDto> getAttendanceHistory(Integer classId) {
        List<Object[]> results = sessionRepository.getAttendanceHistoryByClassId(classId);
        List<AttendanceHistoryDto> dtos = new ArrayList<>();
        for (Object[] row : results) {
            AttendanceHistoryDto dto = new AttendanceHistoryDto();
            dto.setScheduleId((Long) row[0]);
            dto.setModuleName((String) row[1]);
            dto.setSessionNumber((Integer) row[2]);
            dto.setSessionTitle((String) row[3]);
            dto.setSessionDate((LocalDate) row[4]);
            dto.setOpenedAt((LocalDateTime) row[5]);
            dto.setClosedAt((LocalDateTime) row[6]);
            dto.setIsLate((Boolean) row[7]);
            dto.setOpenedByName((String) row[8]);
            dto.setMarkedByName((String) row[9]);
            dtos.add(dto);
        }
        return dtos;
    }

    public List<AttendanceStatsDto> getAttendanceStats(Integer classId) {
        List<Object[]> results = recordRepository.getAttendanceStatsByClassId(classId);
        List<AttendanceStatsDto> dtos = new ArrayList<>();
        for (Object[] row : results) {
            AttendanceStatsDto dto = new AttendanceStatsDto();
            dto.setStudentId((Integer) row[0]);
            dto.setStudentName((String) row[1]);
            dto.setAttendedCount((Integer) row[2]);
            dto.setTotalSessions((Integer) row[3]);
            dto.setAttendanceRate((Double) row[4]);
            dtos.add(dto);
        }
        return dtos;
    }

    @Transactional
    public AttendanceSession openAttendanceSession(Long scheduleId, Integer teacherId) {
        Optional<AttendanceSession> existing = sessionRepository.findByScheduleId(scheduleId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Buổi điểm danh đã được mở trước đó.");
        }

        if (!isTeacherAuthorized(scheduleId, teacherId)) {
            throw new SecurityException("Bạn không có quyền mở điểm danh cho buổi học này.");
        }

        LocalDateTime startDate = getScheduleStartDate(scheduleId);
        boolean isLate = startDate != null && LocalDateTime.now().isAfter(startDate.plusMinutes(15));

        AttendanceSession session = new AttendanceSession();
        session.setScheduleId(scheduleId);
        session.setOpenedBy(teacherId);
        session.setIsLate(isLate);
        session.setOpenedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    @Transactional
    public List<AttendanceRecord> markAttendanceBatch(Long scheduleId, MarkAttendanceRequest request) {
        Integer teacherId = request.getTeacherId();

        AttendanceSession session = sessionRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new IllegalStateException("Chưa mở buổi điểm danh."));

        if (!isTeacherAuthorized(scheduleId, teacherId)) {
            throw new SecurityException("Không có quyền điểm danh.");
        }

        List<AttendanceRecord> savedRecords = new ArrayList<>();

        for (AttendanceMarkDto dto : request.getRecords()) {
            AttendanceRecord record = new AttendanceRecord();
            record.setScheduleId(scheduleId);
            record.setStudentId(dto.getStudentId());
            record.setStatus(AttendanceRecord.AttendanceStatus.valueOf(dto.getStatus()));
            record.setMarkedBy(teacherId);
            record.setNotes(dto.getNotes());

            Optional<AttendanceRecord> existing = recordRepository.findByScheduleIdAndStudentId(scheduleId, dto.getStudentId());
            if (existing.isPresent()) {
                AttendanceRecord e = existing.get();
                e.setStatus(record.getStatus());
                e.setMarkedBy(record.getMarkedBy());
                e.setNotes(record.getNotes());
                e.setMarkedAt(LocalDateTime.now());
                savedRecords.add(recordRepository.save(e));
            } else {
                record.setMarkedAt(LocalDateTime.now());
                savedRecords.add(recordRepository.save(record));
            }
        }

        return savedRecords;
    }

    private boolean isTeacherAuthorized(Long scheduleId, Integer teacherId) {
        Long count = recordRepository.countByScheduleIdAndMarkedBy(scheduleId, teacherId);
        return count > 0 ||
               sessionRepository.findByScheduleId(scheduleId)
                   .map(s -> s.getOpenedBy().equals(teacherId))
                   .orElse(false);
    }

    private LocalDateTime getScheduleStartDate(Long scheduleId) {
        Object result = entityManager.createNativeQuery(
                "SELECT start_date FROM class_module_schedules WHERE id = ?"
            ).setParameter(1, scheduleId)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null);

        if (result instanceof Timestamp) {
            return ((Timestamp) result).toLocalDateTime();
        }
        return null;
    }

}
