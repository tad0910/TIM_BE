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

        // --- Additional test cases for createSchedule ---

        @Test
        void createSchedule_ClassNotFound() {
                when(classRepository.existsById(999L)).thenReturn(false);
                scheduleDTO.setClassId(999L);

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Lớp học không tồn tại");
        }

        @Test
        void createSchedule_ModuleNotFound() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(999)).thenReturn(false);
                scheduleDTO.setModuleId(999);

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Module không tồn tại");
        }

        @Test
        void createSchedule_InstructorNotFound() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(999L)).thenReturn(false);
                scheduleDTO.setInstructorId(999L);

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Giảng viên không tồn tại");
        }

        @Test
        void createSchedule_ClassModuleNotFound() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(classModuleRepository.existsById(999L)).thenReturn(false);
                scheduleDTO.setClassModuleId(999L);

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("ClassModule không tồn tại");
        }

        @Test
        void createSchedule_NullStartDate() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                scheduleDTO.setStartDate(null);

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }

        @Test
        void createSchedule_NullEndDate() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                scheduleDTO.setEndDate(null);

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }

        @Test
        void createSchedule_WithModuleSessionId_AlreadyExists() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                scheduleDTO.setModuleSessionId(100L);

                ClassModuleSchedule existingSchedule = new ClassModuleSchedule();
                existingSchedule.setModuleSessionId(100L);
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(existingSchedule));

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("ModuleSession này đã được lập lịch");
        }

        @Test
        void createSchedule_WithModuleSessionId_Success() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                scheduleDTO.setModuleSessionId(100L);
                scheduleDTO.setInstructorId(3L);

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.emptyList());
                when(scheduleRepository.findConflictingSchedules(anyLong(), any(), any(), eq(0L)))
                                .thenReturn(Collections.emptyList());
                when(scheduleRepository.save(any(ClassModuleSchedule.class))).thenReturn(scheduleEntity);

                ClassModuleScheduleDTO result = scheduleService.createSchedule(scheduleDTO);

                assertThat(result).isNotNull();
                verify(scheduleRepository).save(any(ClassModuleSchedule.class));
        }

        @Test
        void createSchedule_WithInstructor_ModuleSessionsEmpty() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.emptyList());
                when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(2))
                                .thenReturn(Collections.emptyList());

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Module này chưa có ModuleSession nào");
        }

        @Test
        void createSchedule_WithInstructor_ModuleAlreadyScheduled() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);

                ClassModuleSchedule existingSchedule = new ClassModuleSchedule();
                existingSchedule.setModuleId(2);
                existingSchedule.setModuleSessionId(null);
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(existingSchedule));

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Module này đã được lập lịch");
        }

        @Test
        void createSchedule_WithInstructor_ModuleSessionsAlreadyScheduled() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);

                ClassModuleSchedule existingSchedule = new ClassModuleSchedule();
                existingSchedule.setModuleId(2);
                existingSchedule.setModuleSessionId(100L);
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(existingSchedule));

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Module này đã được lập lịch");
        }

        @Test
        void createSchedule_WithoutInstructor_ModuleAlreadyScheduled() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                scheduleDTO.setInstructorId(null);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(true);

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Module này đã được lập lịch");
        }

        @Test
        void createSchedule_WithoutInstructor_Success() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                scheduleDTO.setInstructorId(null);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);
                when(scheduleRepository.save(any(ClassModuleSchedule.class))).thenReturn(scheduleEntity);

                ClassModuleScheduleDTO result = scheduleService.createSchedule(scheduleDTO);

                assertThat(result).isNotNull();
                verify(scheduleRepository).save(any(ClassModuleSchedule.class));
        }

        @Test
        void createSchedule_CheckInstructorConflict_ModuleNotFound() {
                when(classRepository.existsById(1L)).thenReturn(true);
                when(moduleRepository.existsById(2)).thenReturn(true);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);

                ClassModuleSchedule conflictingSchedule = new ClassModuleSchedule();
                conflictingSchedule.setModuleId(99);
                conflictingSchedule.setStartDate(now.plusDays(1));
                when(scheduleRepository.findConflictingSchedules(eq(3L), any(), any(), eq(0L)))
                                .thenReturn(Collections.singletonList(conflictingSchedule));
                when(moduleRepository.findById(99)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> scheduleService.createSchedule(scheduleDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Giảng viên đã bị trùng lịch");
        }

        // --- Additional test cases for updateSchedule ---

        @Test
        void updateSchedule_NullStartDate() {
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                ClassModuleScheduleDTO updateDTO = new ClassModuleScheduleDTO();
                updateDTO.setStartDate(null);
                updateDTO.setEndDate(now.plusDays(2));

                assertThatThrownBy(() -> scheduleService.updateSchedule(10L, updateDTO))
                                .isInstanceOf(InvalidRequestException.class)
                                .hasMessageContaining("Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }

        @Test
        void updateSchedule_ClassModuleNotFound() {
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                when(scheduleRepository.findConflictingSchedules(any(), any(), any(), eq(10L)))
                                .thenReturn(Collections.emptyList());
                when(classModuleRepository.existsById(999L)).thenReturn(false);

                ClassModuleScheduleDTO updateDTO = new ClassModuleScheduleDTO();
                updateDTO.setStartDate(now.plusDays(2));
                updateDTO.setEndDate(now.plusDays(2).plusHours(2));
                updateDTO.setClassModuleId(999L);

                assertThatThrownBy(() -> scheduleService.updateSchedule(10L, updateDTO))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("ClassModule không tồn tại");
        }

        @Test
        void updateSchedule_WithAllFields() {
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                when(scheduleRepository.findConflictingSchedules(any(), any(), any(), eq(10L)))
                                .thenReturn(Collections.emptyList());
                when(classModuleRepository.existsById(5L)).thenReturn(true);
                when(scheduleRepository.save(any(ClassModuleSchedule.class))).thenReturn(scheduleEntity);

                ClassModuleScheduleDTO updateDTO = new ClassModuleScheduleDTO();
                updateDTO.setStartDate(now.plusDays(2));
                updateDTO.setEndDate(now.plusDays(2).plusHours(2));
                updateDTO.setClassModuleId(5L);
                updateDTO.setModuleSessionId(200L);
                updateDTO.setInstructorId(4L);
                updateDTO.setStatus(ClassModuleSchedule.ScheduleStatus.ongoing);

                ClassModuleScheduleDTO result = scheduleService.updateSchedule(10L, updateDTO);

                assertThat(result).isNotNull();
                verify(scheduleRepository).save(scheduleEntity);
        }

        @Test
        void updateSchedule_WithNullFields() {
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                when(scheduleRepository.findConflictingSchedules(any(), any(), any(), eq(10L)))
                                .thenReturn(Collections.emptyList());
                when(scheduleRepository.save(any(ClassModuleSchedule.class))).thenReturn(scheduleEntity);

                ClassModuleScheduleDTO updateDTO = new ClassModuleScheduleDTO();
                updateDTO.setStartDate(now.plusDays(2));
                updateDTO.setEndDate(now.plusDays(2).plusHours(2));
                // All other fields are null

                ClassModuleScheduleDTO result = scheduleService.updateSchedule(10L, updateDTO);

                assertThat(result).isNotNull();
                verify(scheduleRepository).save(scheduleEntity);
        }

        // --- Test cases for getSchedulesByClass ---

        @Test
        void getSchedulesByClass_Success() {
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));
                when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                verify(scheduleRepository).findByClassId(1L);
        }

        @Test
        void getSchedulesByClass_Empty() {
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.emptyList());

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).isEmpty();
        }

        // --- Test cases for getSchedulesByInstructor ---

        @Test
        void getSchedulesByInstructor_Success() {
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorId(3L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByInstructor(3L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                verify(scheduleRepository).findByInstructorId(3L);
        }

        @Test
        void getSchedulesByInstructor_InstructorNotFound() {
                when(userRepository.existsById(999L)).thenReturn(false);

                assertThatThrownBy(() -> scheduleService.getSchedulesByInstructor(999L))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Giảng viên không tồn tại");
        }

        // --- Test cases for getSchedulesByClass with date range ---

        @Test
        void getSchedulesByClass_WithDateRange() {
                LocalDateTime startDate = now.plusDays(1);
                LocalDateTime endDate = now.plusDays(7);
                when(scheduleRepository.findByClassIdAndStartDateBetween(1L, startDate, endDate))
                                .thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));
                when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L, startDate, endDate);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                verify(scheduleRepository).findByClassIdAndStartDateBetween(1L, startDate, endDate);
        }

        @Test
        void getSchedulesByClass_WithoutDateRange() {
                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));
                when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L, null, null);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                verify(scheduleRepository).findByClassId(1L);
        }

        // --- Test cases for getSchedulesByInstructor with date range ---

        @Test
        void getSchedulesByInstructor_WithDateRange() {
                LocalDateTime startDate = now.plusDays(1);
                LocalDateTime endDate = now.plusDays(7);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorIdAndStartDateBetween(3L, startDate, endDate))
                                .thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByInstructor(3L, startDate, endDate);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                verify(scheduleRepository).findByInstructorIdAndStartDateBetween(3L, startDate, endDate);
        }

        @Test
        void getSchedulesByInstructor_WithDateRange_InstructorNotFound() {
                LocalDateTime startDate = now.plusDays(1);
                LocalDateTime endDate = now.plusDays(7);
                when(userRepository.existsById(999L)).thenReturn(false);

                assertThatThrownBy(() -> scheduleService.getSchedulesByInstructor(999L, startDate, endDate))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Giảng viên không tồn tại");
        }

        // --- Test cases for getAllSchedulesByTeacher ---

        @Test
        void getAllSchedulesByTeacher_TeacherNotFound() {
                when(userRepository.existsById(999L)).thenReturn(false);

                assertThatThrownBy(() -> scheduleService.getAllSchedulesByTeacher(999L, null, null))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Giảng viên không tồn tại");
        }

        @Test
        void getAllSchedulesByTeacher_WithDateRange() {
                LocalDateTime startDate = now.plusDays(1);
                LocalDateTime endDate = now.plusDays(7);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorIdAndStartDateBetween(3L, startDate, endDate))
                                .thenReturn(Collections.singletonList(scheduleEntity));
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.emptyList());
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, startDate, endDate);

                assertThat(result).isNotNull();
                verify(scheduleRepository).findByInstructorIdAndStartDateBetween(3L, startDate, endDate);
        }

        @Test
        void getAllSchedulesByTeacher_WithoutDateRange() {
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorId(3L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.emptyList());
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, null, null);

                assertThat(result).isNotNull();
                verify(scheduleRepository).findByInstructorId(3L);
        }

        @Test
        void getAllSchedulesByTeacher_WithTeacherAssignments() {
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorId(3L)).thenReturn(Collections.emptyList());

                com.tim.appTim.entity.ClassModuleScheduleTeacher assignment = 
                        new com.tim.appTim.entity.ClassModuleScheduleTeacher();
                assignment.setClassModuleScheduleId(20L);
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.singletonList(assignment));

                ClassModuleSchedule otherSchedule = new ClassModuleSchedule();
                otherSchedule.setId(20L);
                otherSchedule.setModuleId(5);
                when(scheduleRepository.findById(20L)).thenReturn(Optional.of(otherSchedule));
                when(moduleRepository.findById(5)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, null, null);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
        }

        @Test
        void getAllSchedulesByTeacher_WithTeacherAssignments_WithDateRange_Filtered() {
                LocalDateTime startDate = now.plusDays(1);
                LocalDateTime endDate = now.plusDays(7);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorIdAndStartDateBetween(3L, startDate, endDate))
                        .thenReturn(Collections.emptyList());

                com.tim.appTim.entity.ClassModuleScheduleTeacher assignment = 
                        new com.tim.appTim.entity.ClassModuleScheduleTeacher();
                assignment.setClassModuleScheduleId(20L);
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.singletonList(assignment));

                ClassModuleSchedule otherSchedule = new ClassModuleSchedule();
                otherSchedule.setId(20L);
                otherSchedule.setModuleId(5);
                otherSchedule.setStartDate(now.plusDays(2));
                otherSchedule.setEndDate(now.plusDays(3));
                when(scheduleRepository.findById(20L)).thenReturn(Optional.of(otherSchedule));
                when(moduleRepository.findById(5)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, startDate, endDate);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
        }

        @Test
        void getAllSchedulesByTeacher_WithTeacherAssignments_WithDateRange_Excluded() {
                LocalDateTime startDate = now.plusDays(10);
                LocalDateTime endDate = now.plusDays(15);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorIdAndStartDateBetween(3L, startDate, endDate))
                        .thenReturn(Collections.emptyList());

                com.tim.appTim.entity.ClassModuleScheduleTeacher assignment = 
                        new com.tim.appTim.entity.ClassModuleScheduleTeacher();
                assignment.setClassModuleScheduleId(20L);
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.singletonList(assignment));

                ClassModuleSchedule otherSchedule = new ClassModuleSchedule();
                otherSchedule.setId(20L);
                otherSchedule.setModuleId(5);
                otherSchedule.setStartDate(now.plusDays(2));
                otherSchedule.setEndDate(now.plusDays(3));
                when(scheduleRepository.findById(20L)).thenReturn(Optional.of(otherSchedule));

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, startDate, endDate);

                assertThat(result).isNotNull();
                assertThat(result).isEmpty();
        }

        // --- Test cases for deleteSchedule ---

        @Test
        void deleteSchedule_Success() {
                when(scheduleRepository.existsById(10L)).thenReturn(true);
                doNothing().when(scheduleRepository).deleteById(10L);

                scheduleService.deleteSchedule(10L);

                verify(scheduleRepository).deleteById(10L);
        }

        @Test
        void deleteSchedule_NotFound() {
                when(scheduleRepository.existsById(999L)).thenReturn(false);

                assertThatThrownBy(() -> scheduleService.deleteSchedule(999L))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Lịch học không tồn tại");
        }

        // --- Test cases for convertToDTO ---

        @Test
        void convertToDTO_WithClassEntity() {
                com.tim.appTim.entity.Class classEntity = new com.tim.appTim.entity.Class();
                classEntity.setClassName("Test Class");
                scheduleEntity.setClassEntity(classEntity);

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));
                when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getClassName()).isEqualTo("Test Class");
        }

        @Test
        void convertToDTO_WithModule() {
                Module module = new Module();
                module.setName("Test Module");
                scheduleEntity.setModule(module);

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getModuleName()).isEqualTo("Test Module");
        }

        @Test
        void convertToDTO_WithInstructor() {
                User instructor = new User();
                instructor.setUsername("test_instructor");
                scheduleEntity.setInstructor(instructor);

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getInstructorName()).isEqualTo("test_instructor");
        }

        @Test
        void convertToDTO_LoadModuleFromRepository() {
                scheduleEntity.setModule(null);
                Module module = new Module();
                module.setName("Loaded Module");
                when(moduleRepository.findById(2)).thenReturn(Optional.of(module));

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getModuleName()).isEqualTo("Loaded Module");
        }

        @Test
        void convertToDTO_LoadInstructorFromRepository() {
                scheduleEntity.setInstructor(null);
                User instructor = new User();
                instructor.setUsername("loaded_instructor");
                when(userRepository.findById(3L)).thenReturn(Optional.of(instructor));

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getInstructorName()).isEqualTo("loaded_instructor");
        }

        @Test
        void convertToDTO_WithNullModuleId() {
                scheduleEntity.setModuleId(null);
                scheduleEntity.setModule(null);

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
        }

        @Test
        void convertToDTO_WithNullInstructorId() {
                scheduleEntity.setInstructorId(null);
                scheduleEntity.setInstructor(null);

                when(scheduleRepository.findByClassId(1L)).thenReturn(Collections.singletonList(scheduleEntity));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getSchedulesByClass(1L);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(1);
        }

        @Test
        void getAllSchedulesByTeacher_WithNullScheduleIdInAssignment() {
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorId(3L)).thenReturn(Collections.emptyList());

                com.tim.appTim.entity.ClassModuleScheduleTeacher assignment = 
                        new com.tim.appTim.entity.ClassModuleScheduleTeacher();
                assignment.setClassModuleScheduleId(null);
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.singletonList(assignment));

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, null, null);

                assertThat(result).isNotNull();
                assertThat(result).isEmpty();
        }

        @Test
        void getAllSchedulesByTeacher_WithDateRange_NullDates() {
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorId(3L)).thenReturn(Collections.singletonList(scheduleEntity));

                com.tim.appTim.entity.ClassModuleScheduleTeacher assignment = 
                        new com.tim.appTim.entity.ClassModuleScheduleTeacher();
                assignment.setClassModuleScheduleId(20L);
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.singletonList(assignment));

                ClassModuleSchedule otherSchedule = new ClassModuleSchedule();
                otherSchedule.setId(20L);
                otherSchedule.setModuleId(5);
                // Mock findById for both schedules (10L from instructor and 20L from assignment)
                when(scheduleRepository.findById(10L)).thenReturn(Optional.of(scheduleEntity));
                when(scheduleRepository.findById(20L)).thenReturn(Optional.of(otherSchedule));
                when(moduleRepository.findById(2)).thenReturn(Optional.of(new Module()));
                when(moduleRepository.findById(5)).thenReturn(Optional.of(new Module()));

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, null, null);

                assertThat(result).isNotNull();
                assertThat(result).hasSize(2);
        }

        @Test
        void getAllSchedulesByTeacher_WithDateRange_ScheduleNotFound() {
                LocalDateTime startDate = now.plusDays(1);
                LocalDateTime endDate = now.plusDays(7);
                when(userRepository.existsById(3L)).thenReturn(true);
                when(scheduleRepository.findByInstructorIdAndStartDateBetween(3L, startDate, endDate))
                        .thenReturn(Collections.emptyList());

                com.tim.appTim.entity.ClassModuleScheduleTeacher assignment = 
                        new com.tim.appTim.entity.ClassModuleScheduleTeacher();
                assignment.setClassModuleScheduleId(20L);
                when(scheduleTeacherRepository.findByUserId(3L)).thenReturn(Collections.singletonList(assignment));
                when(scheduleRepository.findById(20L)).thenReturn(Optional.empty());

                List<ClassModuleScheduleDTO> result = scheduleService.getAllSchedulesByTeacher(3L, startDate, endDate);

                assertThat(result).isNotNull();
                assertThat(result).isEmpty();
        }
}
