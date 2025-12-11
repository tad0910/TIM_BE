package com.tim.appTim.service;

import com.tim.appTim.dto.MarkAttendanceRequest;
import com.tim.appTim.dto.AttendanceDetailDto;
import com.tim.appTim.dto.AttendanceHistoryDto;
import com.tim.appTim.dto.AttendanceMarkDto;
import com.tim.appTim.dto.AttendanceStatsDto;
import com.tim.appTim.entity.AttendanceSession;
import com.tim.appTim.entity.AttendanceRecord;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.AttendanceSessionRepository;
import com.tim.appTim.repository.AttendanceRecordRepository;
import com.tim.appTim.service.BehaviorLookupService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import com.tim.appTim.entity.User;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;


@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final UserService userService;
    private final BehaviorLookupService behaviorLookupService;
    private final GamificationService gamificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             AttendanceRecordRepository recordRepository, 
                             UserService userService,
                             @Lazy GamificationService gamificationService,
                             BehaviorLookupService behaviorLookupService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.userService = userService;
        this.gamificationService = gamificationService;
        this.behaviorLookupService = behaviorLookupService;
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

        Timestamp sessionTs = (Timestamp) row[4];
        dto.setSessionDatetime(sessionTs != null ? sessionTs.toLocalDateTime() : null);

        Timestamp openedAtTs = (Timestamp) row[5];
        dto.setOpenedAt(openedAtTs != null ? openedAtTs.toLocalDateTime() : null);

        Timestamp closedAtTs = (Timestamp) row[6];
        dto.setClosedAt(closedAtTs != null ? closedAtTs.toLocalDateTime() : null);
        
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
            dto.setStudentId(row[0] instanceof Number ? ((Number) row[0]).longValue() : null);
            dto.setStudentName((String) row[1]);
            dto.setAttendedCount(row[2] instanceof Number ? ((Number) row[2]).longValue() : null);
            dto.setTotalSessions(row[3] instanceof Number ? ((Number) row[3]).longValue() : null);

            Object rateObj = row[4];
            if (rateObj instanceof BigDecimal) {
                dto.setAttendanceRate(((BigDecimal) rateObj).doubleValue());
            } else if (rateObj instanceof Double) {
                dto.setAttendanceRate((Double) rateObj);
            } else {
                dto.setAttendanceRate(null);
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @Transactional
    public AttendanceSession openAttendanceSession(Long scheduleId, Integer teacherId, Authentication authentication) {
        if (teacherId == null) {
            throw new BadRequestException("teacherId là bắt buộc");
        }

        boolean isAdminOverride = false; 

        if (authentication != null && authentication.isAuthenticated()) {
            isAdminOverride = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("attendance:open"));
        }

        if (!isAdminOverride && !isTeacherAuthorized(scheduleId, teacherId)) {
            throw new ForbiddenException("Bạn không có quyền mở điểm danh cho buổi học này.");
        }


        Optional<AttendanceSession> existing = sessionRepository.findByScheduleId(scheduleId);
        
        LocalDateTime startDate = getScheduleStartDate(scheduleId);
        boolean isLate = startDate != null && LocalDateTime.now().isAfter(startDate.plusMinutes(15));

        if (existing.isPresent()) {
            long markedCount = recordRepository.countByScheduleId(scheduleId);
            if (markedCount > 0) {

                throw new ConflictException("Buổi điểm danh đã có sinh viên được đánh dấu, không thể mở lại.");
            }

            AttendanceSession session = existing.get();
            session.setOpenedBy(teacherId); 
            session.setIsLate(isLate);     
            session.setOpenedAt(LocalDateTime.now()); 
            return sessionRepository.save(session);

        } else {
            AttendanceSession session = new AttendanceSession();
            session.setScheduleId(scheduleId);
            session.setOpenedBy(teacherId);
            session.setIsLate(isLate);
            session.setOpenedAt(LocalDateTime.now());
            return sessionRepository.save(session);
        }
    }


    @Transactional
    public List<AttendanceRecord> markAttendanceBatch(Long scheduleId, MarkAttendanceRequest request, Authentication authentication) {
        Integer teacherId = request.getTeacherId();
        if (teacherId == null) {
            throw new BadRequestException("teacherId là bắt buộc");
        }

        AttendanceSession session = sessionRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa mở buổi điểm danh."));

        boolean isAdminOverride = false;

        if (authentication != null && authentication.isAuthenticated()) {
            isAdminOverride = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("attendance:mark"));
        }

        if (!isAdminOverride && !isTeacherAuthorized(scheduleId, teacherId)) {
            throw new ForbiddenException("Không có quyền điểm danh.");
        }

        List<AttendanceRecord> savedRecords = new ArrayList<>();

        for (AttendanceMarkDto dto : request.getRecords()) {
            if (dto.getStudentId() == null || dto.getStatus() == null) {
                throw new BadRequestException("studentId và status là bắt buộc cho mỗi bản ghi điểm danh.");
            }

            AttendanceRecord record = new AttendanceRecord();
            record.setScheduleId(scheduleId);
            record.setStudentId(dto.getStudentId());
            try {

                String statusStr = dto.getStatus();

                if (statusStr != null) {
                    statusStr = statusStr.trim().toLowerCase();
                }

                record.setStatus(AttendanceRecord.AttendanceStatus.valueOf(statusStr));
                
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + dto.getStatus() + ". Các trạng thái hợp lệ: PRESENT, ABSENT, LATE, EXCUSED.");
            }
            record.setMarkedBy(teacherId);
            record.setNotes(dto.getNotes());

            Optional<AttendanceRecord> existing = recordRepository.findByScheduleIdAndStudentId(scheduleId, dto.getStudentId());
            AttendanceRecord savedRecord;
            if (existing.isPresent()) {
                AttendanceRecord e = existing.get();
                e.setStatus(record.getStatus());
                e.setMarkedBy(record.getMarkedBy());
                e.setNotes(record.getNotes());
                e.setMarkedAt(LocalDateTime.now());
                savedRecord = recordRepository.save(e);
            } else {
                record.setMarkedAt(LocalDateTime.now());
                savedRecord = recordRepository.save(record);
            }
            savedRecords.add(savedRecord);

            if (savedRecord.getStatus() == AttendanceRecord.AttendanceStatus.present && 
                !session.getIsLate()) {
                try {
                    Integer behaviorId = behaviorLookupService.getIdByName("Điểm danh đúng giờ");
                    gamificationService.awardPoints(savedRecord.getStudentId().longValue(), behaviorId);
                } catch (Exception e) {

                    System.err.println("Failed to award points for attendance: " + e.getMessage());
                }
            }
        }

        return savedRecords;
    }

    public List<AttendanceDetailDto> getAttendanceDetails(Long scheduleId) {
        List<Object[]> results = recordRepository.getAttendanceDetailsByScheduleId(scheduleId);
        List<AttendanceDetailDto> dtos = new ArrayList<>();

        for (Object[] row : results) {
            AttendanceDetailDto dto = new AttendanceDetailDto();
            dto.setStudentId(row[0] instanceof Number ? ((Number) row[0]).longValue() : null);
            dto.setStudentName((String) row[1]);
            String statusStr = (String) row[2];
            dto.setStatus(statusStr != null ? statusStr.toLowerCase() : "absent"); 
            Timestamp markedAtTs = (Timestamp) row[3];
            dto.setMarkedAt(markedAtTs != null ? markedAtTs.toLocalDateTime() : null);
            dto.setNotes((String) row[4]);
            Object markedByObj = row[5];
            dto.setMarkedBy(markedByObj instanceof Number ? ((Number) markedByObj).intValue() : null);
            dtos.add(dto);
        }
        return dtos;
    }

    private boolean isTeacherAuthorized(Long scheduleId, Integer teacherId) {
    Long markedCount = recordRepository.countByScheduleIdAndMarkedBy(scheduleId, teacherId);
    if (markedCount != null && markedCount > 0) {
        return true;
    }

        boolean openedByTeacher = sessionRepository.findByScheduleId(scheduleId)
            .map(session -> teacherId.equals(session.getOpenedBy()))
            .orElse(Boolean.FALSE);

        if (openedByTeacher) {
            return true;
        }

        Object result = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM class_module_schedules WHERE id = ? AND instructor_id = ?"
            )
            .setParameter(1, scheduleId)
            .setParameter(2, teacherId)
            .getSingleResult();

        Long assignedCount = (result instanceof Number) ? ((Number) result).longValue() : 0L;

        return assignedCount > 0;
    }

    public boolean isScheduleTeacher(Authentication authentication, Long scheduleId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null || currentUser.getId() == null) {
            return false;
        }

        Integer teacherId;
        try {
            teacherId = currentUser.getId().intValue();
        } catch (Exception e) {
            return false;
        }

        return isTeacherAuthorized(scheduleId, teacherId);
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