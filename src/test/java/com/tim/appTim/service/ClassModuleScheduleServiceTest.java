package com.tim.appTim.service;

import com.tim.appTim.dto.ClassModuleScheduleDTO;
import com.tim.appTim.entity.ClassModuleSchedule;
import com.tim.appTim.entity.Module;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.InvalidRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassModuleScheduleServiceTest {

        @Mock
        private ClassModuleScheduleRepository scheduleRepository;
        @Mock
        private ModuleRepository moduleRepository;
        @Mock
        private ModuleSessionRepository moduleSessionRepository;
        @Mock
        private ClassRepository classRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private ClassModuleRepository classModuleRepository;
        @Mock
        private ClassModuleScheduleTeacherRepository scheduleTeacherRepository;

        @InjectMocks
        private ClassModuleScheduleService scheduleService;

        private ClassModuleScheduleDTO scheduleDTO;
        private ClassModuleSchedule scheduleEntity;
        private LocalDateTime now;

        @BeforeEach
        void setUp() {
                now = LocalDateTime.now();
                scheduleDTO = new ClassModuleScheduleDTO();
                scheduleDTO.setClassId(1L);
                scheduleDTO.setModuleId(2);
                scheduleDTO.setInstructorId(3L);
                scheduleDTO.setStartDate(now.plusDays(1));
                scheduleDTO.setEndDate(now.plusDays(1).plusHours(2));

                scheduleEntity = new ClassModuleSchedule();
                scheduleEntity.setId(10L);
                scheduleEntity.setClassId(1L);
                scheduleEntity.setModuleId(2);
                scheduleEntity.setInstructorId(3L);
                scheduleEntity.setStartDate(now.plusDays(1));
                scheduleEntity.setEndDate(now.plusDays(1).plusHours(2));

                ModuleSession session = new ModuleSession();
                session.setId(100L);
                when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(2))
                                .thenReturn(Collections.singletonList(session));
        }

        // --- createSchedule ---

        @Test
        // Covers: Create Schedule (Success)
        void createSchedule_Success() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);
                when(scheduleRepository.findConflictingSchedules(anyLong(), any(), any(), eq(0L)))
                                .thenReturn(Collections.emptyList());
                when(scheduleRepository.saveAll(anyList())).thenReturn(Collections.singletonList(scheduleEntity));

                ClassModuleScheduleDTO result = scheduleService.createSchedule(scheduleDTO);

                assertThat(result).isNotNull();
                verify(scheduleRepository).saveAll(anyList());
        }

        @Test
        // Covers: Create Schedule (Fail - Invalid Date Range)
        void createSchedule_InvalidDateRange() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);

                scheduleDTO.setEndDate(scheduleDTO.getStartDate().minusHours(1)); // End before start

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessage("Ngày kết thúc không thể trước ngày bắt đầu.");
        }

        @Test
        // Covers: Create Schedule (Fail - Overlap Case A: New starts inside existing)
        void createSchedule_Overlap_StartInside() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);

                ClassModuleSchedule conflictingSchedule = new ClassModuleSchedule();
                conflictingSchedule.setModuleId(99);
                conflictingSchedule.setStartDate(now.plusDays(1).minusHours(1));
                when(scheduleRepository.findConflictingSchedules(eq(3L), any(), any(), eq(0L)))
                                .thenReturn(Collections.singletonList(conflictingSchedule));

                when(moduleRepository.findById(99)).thenReturn(Optional.of(new Module())); // For error message

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Giảng viên đã bị trùng lịch");
        }

        @Test
        // Covers: Create Schedule (Fail - Overlap Case B: New ends inside existing)
        // Note: The service uses findConflictingSchedules which should handle all
        // overlap logic.
        // We mock the repository response to simulate the DB finding a conflict.
        void createSchedule_Overlap_EndInside() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);

                ClassModuleSchedule conflictingSchedule = new ClassModuleSchedule();
                conflictingSchedule.setModuleId(99);
                when(scheduleRepository.findConflictingSchedules(eq(3L), any(), any(), eq(0L)))
                                .thenReturn(Collections.singletonList(conflictingSchedule));
                when(moduleRepository.findById(99)).thenReturn(Optional.of(new Module()));

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Giảng viên đã bị trùng lịch");
        }

        @Test
        // Covers: Create Schedule (Fail - Overlap Case C: New engulfs existing)
        void createSchedule_Overlap_Engulfs() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);

                ClassModuleSchedule conflictingSchedule = new ClassModuleSchedule();
                conflictingSchedule.setModuleId(99);
                when(scheduleRepository.findConflictingSchedules(eq(3L), any(), any(), eq(0L)))
                                .thenReturn(Collections.singletonList(conflictingSchedule));
                when(moduleRepository.findById(99)).thenReturn(Optional.of(new Module()));

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Giảng viên đã bị trùng lịch");
        }

        @Test
        // Covers: Create Schedule with Module Sessions (Success)
        void createSchedule_WithSessions_Success() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);

                // Mock existing checks
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.emptyList());

                // Mock sessions
                ModuleSession session1 = new ModuleSession();
                session1.setId(101L);
                ModuleSession session2 = new ModuleSession();
                session2.setId(102L);
                when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(2))
                                .thenReturn(List.of(session1, session2));

                when(scheduleRepository.findConflictingSchedules(anyLong(), any(), any(), eq(0L)))
                                .thenReturn(Collections.emptyList());

                List<ClassModuleSchedule> savedList = new ArrayList<>();
                savedList.add(scheduleEntity);
                when(scheduleRepository.saveAll(anyList())).thenReturn(savedList);

                ClassModuleScheduleDTO result = scheduleService.createSchedule(scheduleDTO);

                assertThat(result).isNotNull();
                verify(scheduleRepository).saveAll(anyList());
        }

        // --- updateSchedule ---

        @Test
        // Covers: Update Schedule (Success)
        void updateSchedule_Success() {
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                when(scheduleRepository.findConflictingSchedules(eq(3L), any(), any(), eq(10L))) // Pass ID to exclude
                                                                                                 // self
                                .thenReturn(Collections.emptyList());
                when(scheduleRepository.save(any(ClassModuleSchedule.class))).thenReturn(scheduleEntity);

                ClassModuleScheduleDTO updateDTO = new ClassModuleScheduleDTO();
                updateDTO.setStartDate(now.plusDays(2));
                updateDTO.setEndDate(now.plusDays(2).plusHours(2));
                updateDTO.setInstructorId(3L);

                ClassModuleScheduleDTO result = scheduleService.updateSchedule(10L, updateDTO);

                assertThat(result).isNotNull();
                verify(scheduleRepository).save(scheduleEntity);
        }

        @Test
        // Covers: Update Schedule (Fail - Conflict with OTHER schedule)
        void updateSchedule_Conflict() {
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));

                ClassModuleSchedule conflictingSchedule = new ClassModuleSchedule();
                conflictingSchedule.setModuleId(99);
                conflictingSchedule.setId(20L); // Different ID

                when(scheduleRepository.findConflictingSchedules(eq(3L), any(), any(), eq(10L)))
                                .thenReturn(Collections.singletonList(conflictingSchedule));
                when(moduleRepository.findById(99)).thenReturn(Optional.of(new Module()));

                ClassModuleScheduleDTO updateDTO = new ClassModuleScheduleDTO();
                updateDTO.setStartDate(now.plusDays(2));
                updateDTO.setEndDate(now.plusDays(2).plusHours(2));
                updateDTO.setInstructorId(3L);

                assertThatThrownBy(() -> scheduleService.updateSchedule(10L, updateDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Giảng viên đã bị trùng lịch");
        }

        @Test
        // Covers: Update Schedule (Fail - Not Found)
        void updateSchedule_NotFound() {
                when(scheduleRepository.findById(999L)).thenReturn(Optional.empty());

                ClassModuleScheduleDTO updateDTO = new ClassModuleScheduleDTO();
                assertThatThrownBy(() -> scheduleService.updateSchedule(999L, updateDTO))
                                .isInstanceOf(ResourceNotFoundException.class);
        }

        // --- Teacher Availability (Implicit in create/update via
        // checkInstructorConflict) ---

        @Test
        // Covers: Teacher Availability - Assigning busy teacher
        void createSchedule_TeacherBusy() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);

                ClassModuleSchedule conflictingSchedule = new ClassModuleSchedule();
                conflictingSchedule.setModuleId(99);
                when(scheduleRepository.findConflictingSchedules(eq(3L), any(), any(), eq(0L)))
                                .thenReturn(Collections.singletonList(conflictingSchedule));
                when(moduleRepository.findById(99)).thenReturn(Optional.of(new Module()));

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Giảng viên đã bị trùng lịch");
        }
}
