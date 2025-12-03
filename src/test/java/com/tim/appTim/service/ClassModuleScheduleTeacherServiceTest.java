package com.tim.appTim.service;

import com.tim.appTim.dto.ClassModuleScheduleTeacherDTO;
import com.tim.appTim.entity.ClassModuleScheduleTeacher;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassModuleScheduleRepository;
import com.tim.appTim.repository.ClassModuleScheduleTeacherRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassModuleScheduleTeacherServiceTest {

    @Mock
    private ClassModuleScheduleTeacherRepository scheduleTeacherRepository;
    @Mock
    private ClassModuleScheduleRepository scheduleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClassModuleScheduleTeacherService scheduleTeacherService;

    private User teacher;
    private ClassModuleScheduleTeacher scheduleTeacher;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(10L);
        teacher.setUsername("teacher1");
        teacher.setEmail("teacher1@example.com");

        scheduleTeacher = new ClassModuleScheduleTeacher();
        scheduleTeacher.setId(1L);
        scheduleTeacher.setClassModuleScheduleId(1L);
        scheduleTeacher.setUserId(10L);
        scheduleTeacher.setRole(ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER);
        scheduleTeacher.setUser(teacher);
        scheduleTeacher.setAssignedAt(LocalDateTime.now());
    }

    // --- assignTeacherToSchedule ---

    @Test
    void assignTeacherToSchedule_Success() {
        // Arrange
        ClassModuleScheduleTeacherDTO dto = new ClassModuleScheduleTeacherDTO();
        dto.setUserId(10L);
        dto.setRole(ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER);

        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(scheduleTeacherRepository.existsByClassModuleScheduleIdAndUserId(1L, 10L)).thenReturn(false);
        when(scheduleTeacherRepository.save(any(ClassModuleScheduleTeacher.class))).thenAnswer(invocation -> {
            ClassModuleScheduleTeacher st = invocation.getArgument(0);
            st.setId(1L);
            return st;
        });

        // Act
        ClassModuleScheduleTeacherDTO result = scheduleTeacherService.assignTeacherToSchedule(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getRole()).isEqualTo(ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER);
        verify(scheduleTeacherRepository).save(any(ClassModuleScheduleTeacher.class));
    }

    @Test
    void assignTeacherToSchedule_ScheduleNotFound() {
        // Arrange
        ClassModuleScheduleTeacherDTO dto = new ClassModuleScheduleTeacherDTO();
        dto.setUserId(10L);

        when(scheduleRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> scheduleTeacherService.assignTeacherToSchedule(999L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ClassModuleSchedule");
    }

    @Test
    void assignTeacherToSchedule_NullUserId() {
        // Arrange
        ClassModuleScheduleTeacherDTO dto = new ClassModuleScheduleTeacherDTO();
        dto.setUserId(null);

        when(scheduleRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> scheduleTeacherService.assignTeacherToSchedule(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("userId là bắt buộc");
    }

    @Test
    void assignTeacherToSchedule_UserNotFound() {
        // Arrange
        ClassModuleScheduleTeacherDTO dto = new ClassModuleScheduleTeacherDTO();
        dto.setUserId(999L);

        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> scheduleTeacherService.assignTeacherToSchedule(1L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy người dùng");
    }

    @Test
    void assignTeacherToSchedule_AlreadyAssigned() {
        // Arrange
        ClassModuleScheduleTeacherDTO dto = new ClassModuleScheduleTeacherDTO();
        dto.setUserId(10L);

        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(scheduleTeacherRepository.existsByClassModuleScheduleIdAndUserId(1L, 10L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> scheduleTeacherService.assignTeacherToSchedule(1L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Giáo viên đã được gán vào buổi học này");
    }

    @Test
    void assignTeacherToSchedule_NullRole() {
        // Arrange
        ClassModuleScheduleTeacherDTO dto = new ClassModuleScheduleTeacherDTO();
        dto.setUserId(10L);
        dto.setRole(null);

        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(scheduleTeacherRepository.existsByClassModuleScheduleIdAndUserId(1L, 10L)).thenReturn(false);
        when(scheduleTeacherRepository.save(any(ClassModuleScheduleTeacher.class))).thenAnswer(invocation -> {
            ClassModuleScheduleTeacher st = invocation.getArgument(0);
            st.setId(1L);
            return st;
        });

        // Act
        ClassModuleScheduleTeacherDTO result = scheduleTeacherService.assignTeacherToSchedule(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER); // Default role
    }

    // --- getScheduleTeachers ---

    @Test
    void getScheduleTeachers_Success() {
        // Arrange
        List<ClassModuleScheduleTeacher> teachers = List.of(scheduleTeacher);
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(teachers);

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
        assertThat(result.get(0).getUserName()).isEqualTo("teacher1");
        assertThat(result.get(0).getUserEmail()).isEqualTo("teacher1@example.com");
    }

    @Test
    void getScheduleTeachers_ScheduleNotFound() {
        // Arrange
        when(scheduleRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> scheduleTeacherService.getScheduleTeachers(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ClassModuleSchedule");
    }

    @Test
    void getScheduleTeachers_Empty() {
        // Arrange
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void getScheduleTeachers_MultipleTeachers() {
        // Arrange
        ClassModuleScheduleTeacher teacher2 = new ClassModuleScheduleTeacher();
        teacher2.setId(2L);
        teacher2.setClassModuleScheduleId(1L);
        teacher2.setUserId(20L);
        teacher2.setRole(ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER);
        
        User teacher2User = new User();
        teacher2User.setId(20L);
        teacher2User.setUsername("teacher2");
        teacher2User.setEmail("teacher2@example.com");
        teacher2.setUser(teacher2User);

        List<ClassModuleScheduleTeacher> teachers = List.of(scheduleTeacher, teacher2);
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(teachers);

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
    }

    // --- removeTeacherFromSchedule ---

    @Test
    void removeTeacherFromSchedule_Success() {
        // Arrange
        when(scheduleTeacherRepository.findByClassModuleScheduleIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(scheduleTeacher));
        doNothing().when(scheduleTeacherRepository).delete(scheduleTeacher);

        // Act
        scheduleTeacherService.removeTeacherFromSchedule(1L, 10L);

        // Assert
        verify(scheduleTeacherRepository).delete(scheduleTeacher);
    }

    @Test
    void removeTeacherFromSchedule_NotFound() {
        // Arrange
        when(scheduleTeacherRepository.findByClassModuleScheduleIdAndUserId(1L, 10L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> scheduleTeacherService.removeTeacherFromSchedule(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy giáo viên");
    }

    // --- updateTeacherRole ---

    @Test
    void updateTeacherRole_Success() {
        // Arrange
        ClassModuleScheduleTeacher.ScheduleTeacherRole newRole = ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER;
        when(scheduleTeacherRepository.findByClassModuleScheduleIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(scheduleTeacher));
        when(scheduleTeacherRepository.save(any(ClassModuleScheduleTeacher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassModuleScheduleTeacherDTO result = scheduleTeacherService.updateTeacherRole(1L, 10L, newRole);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(newRole);
        verify(scheduleTeacherRepository).save(scheduleTeacher);
    }

    @Test
    void updateTeacherRole_NotFound() {
        // Arrange
        when(scheduleTeacherRepository.findByClassModuleScheduleIdAndUserId(1L, 10L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> scheduleTeacherService.updateTeacherRole(1L, 10L, 
                ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy giáo viên");
    }

    // --- convertToDTO tests ---

    @Test
    void convertToDTO_WithUser() {
        // Arrange
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(List.of(scheduleTeacher));

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isEqualTo("teacher1");
        assertThat(result.get(0).getUserEmail()).isEqualTo("teacher1@example.com");
    }

    @Test
    void convertToDTO_WithoutUser_LoadFromRepository() {
        // Arrange
        scheduleTeacher.setUser(null);
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(List.of(scheduleTeacher));
        when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isEqualTo("teacher1");
        verify(userRepository).findById(10L);
    }

    @Test
    void convertToDTO_UserNotFound() {
        // Arrange
        scheduleTeacher.setUser(null);
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(List.of(scheduleTeacher));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isNull();
        assertThat(result.get(0).getUserEmail()).isNull();
    }

    @Test
    void convertToDTO_NullUserId() {
        // Arrange
        scheduleTeacher.setUser(null);
        scheduleTeacher.setUserId(null);
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(List.of(scheduleTeacher));

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isNull();
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void convertToDTO_AllFields() {
        // Arrange
        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(scheduleTeacherRepository.findByClassModuleScheduleId(1L)).thenReturn(List.of(scheduleTeacher));

        // Act
        List<ClassModuleScheduleTeacherDTO> result = scheduleTeacherService.getScheduleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        ClassModuleScheduleTeacherDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getClassModuleScheduleId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(10L);
        assertThat(dto.getRole()).isEqualTo(ClassModuleScheduleTeacher.ScheduleTeacherRole.TEACHER);
        assertThat(dto.getAssignedAt()).isNotNull();
    }
}

