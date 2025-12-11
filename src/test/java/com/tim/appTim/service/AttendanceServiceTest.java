package com.tim.appTim.service;

import com.tim.appTim.dto.AttendanceMarkDto;
import com.tim.appTim.dto.MarkAttendanceRequest;
import com.tim.appTim.entity.*;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private ClassModuleScheduleRepository scheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GamificationService gamificationService;
    @Mock
    private BehaviorLookupService behaviorLookupService;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private AttendanceService attendanceService;

    private User teacher;
    private User student;
    private ClassModuleSchedule schedule;
    private AttendanceSession session;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(1L);
        teacher.setUsername("teacher");

        schedule = new ClassModuleSchedule();
        schedule.setId(1L);
        schedule.setStartDate(LocalDateTime.now().minusHours(1));
        schedule.setEndDate(LocalDateTime.now().plusHours(1));

        session = new AttendanceSession();
        session.setId(1L);
        session.setScheduleId(1L);
        session.setOpenedAt(LocalDateTime.now());

        // Manually inject entityManager because @InjectMocks might be skipping it due
        // to mixed injection
        org.springframework.test.util.ReflectionTestUtils.setField(attendanceService, "entityManager", entityManager);

        lenient().when(attendanceSessionRepository.save(any(AttendanceSession.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Mocking isTeacherAuthorized logic if possible, or assuming it passes for
        // openedBy check
        // Since isTeacherAuthorized checks DB, we might need to mock recordRepository
        // count or sessionRepository find
        // But for simplicity, let's assume the service method logic allows it if we
        // mock correctly.
        // Actually, openAttendanceSession calls isTeacherAuthorized.
        // We need to mock sessionRepository.findByScheduleId(1L) to return empty first
        // (for new session)
        // And for isTeacherAuthorized, we need to mock
        // recordRepository.countByScheduleIdAndMarkedBy or native query.
        // This is hard to mock with just Mockito if it uses EntityManager native query.
        // However, isTeacherAuthorized also checks:
        // boolean openedByTeacher =
        // sessionRepository.findByScheduleId(scheduleId).map(...).orElse(false);
        // If we want to test "opening" a new session, existing is empty.
        // So it falls back to native query or record count.

        // Let's try to mock recordRepository.countByScheduleIdAndMarkedBy to return > 0
        // to bypass native query
        when(attendanceRecordRepository.countByScheduleIdAndMarkedBy(1L, 1)).thenReturn(1L);
        when(attendanceSessionRepository.findByScheduleId(1L)).thenReturn(Optional.empty());

        // Mock EntityManager for getScheduleStartDate
        jakarta.persistence.Query mockQuery = mock(jakarta.persistence.Query.class);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        lenient().when(mockQuery.setParameter(anyInt(), any())).thenReturn(mockQuery);
        lenient().when(mockQuery.getSingleResult()).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.now()));
        lenient().when(mockQuery.getResultList()).thenReturn(List.of(java.sql.Timestamp.valueOf(LocalDateTime.now())));

        AttendanceSession result = attendanceService.openAttendanceSession(1L, 1, null);

        assertNotNull(result);
        verify(attendanceSessionRepository, times(1)).save(any(AttendanceSession.class));
    }

    @Test
    void testMarkAttendanceBatch_Success() {
        MarkAttendanceRequest request = new MarkAttendanceRequest();
        request.setTeacherId(1);
        List<AttendanceMarkDto> dtos = new ArrayList<>();
        AttendanceMarkDto dto = new AttendanceMarkDto();
        dto.setStudentId(2);
        dto.setStatus("PRESENT");
        dtos.add(dto);
        request.setRecords(dtos);

        when(attendanceSessionRepository.findByScheduleId(1L)).thenReturn(Optional.of(session));
        // Mock authorization
        when(attendanceRecordRepository.countByScheduleIdAndMarkedBy(1L, 1)).thenReturn(1L);
        when(behaviorLookupService.getIdByName("Điểm danh đúng giờ")).thenReturn(1);

        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(i -> {
            AttendanceRecord r = i.getArgument(0);
            r.setStudentId(2); // Ensure ID is set
            return r;
        });

        attendanceService.markAttendanceBatch(1L, request, null);

        verify(attendanceRecordRepository, times(1)).save(any(AttendanceRecord.class));
        verify(gamificationService, times(1)).awardPoints(eq(2L), eq(1));
    }

    @Test
    void testMarkAttendanceBatch_Late() {
        MarkAttendanceRequest request = new MarkAttendanceRequest();
        request.setTeacherId(1);
        List<AttendanceMarkDto> dtos = new ArrayList<>();
        AttendanceMarkDto dto = new AttendanceMarkDto();
        dto.setStudentId(2);
        dto.setStatus("LATE");
        dtos.add(dto);
        request.setRecords(dtos);

        when(attendanceSessionRepository.findByScheduleId(1L)).thenReturn(Optional.of(session));
        when(attendanceRecordRepository.countByScheduleIdAndMarkedBy(1L, 1)).thenReturn(1L);
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(i -> i.getArgument(0));

        attendanceService.markAttendanceBatch(1L, request, null);

        verify(attendanceRecordRepository, times(1)).save(any(AttendanceRecord.class));
        verify(gamificationService, never()).awardPoints(eq(2L), eq(1));
    }
}
