package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.Class;
import com.tim.appTim.entity.ClassModule;
import com.tim.appTim.entity.ClassModuleTeacher;
import com.tim.appTim.entity.Module;
import com.tim.appTim.entity.ProgramModule;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ClassModuleRepository;
import com.tim.appTim.repository.ClassModuleTeacherRepository;
import com.tim.appTim.repository.ClassRepository;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ProgramModuleRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassModuleServiceTest {

    @Mock
    private ClassModuleRepository classModuleRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private ClassModuleTeacherRepository classModuleTeacherRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProgramModuleRepository programModuleRepository;

    @InjectMocks
    private ClassModuleService classModuleService;

    private Class classEntity;
    private Module module;
    private ClassModule classModule;
    private ProgramModule programModule;
    private User teacher;
    private ClassModuleTeacher classModuleTeacher;

    @BeforeEach
    void setUp() {
        classEntity = new Class();
        classEntity.setId(1L);
        classEntity.setClassName("Test Class");
        classEntity.setProgramId(1);

        module = new Module();
        module.setId(1);
        module.setName("Test Module");

        classModule = new ClassModule();
        classModule.setId(1L);
        classModule.setClassId(1L);
        classModule.setModuleId(1);
        classModule.setScheduleType(ClassModule.ScheduleType.fixed);
        classModule.setClassEntity(classEntity);
        classModule.setModule(module);

        programModule = new ProgramModule();
        programModule.setModule(module);

        teacher = new User();
        teacher.setId(10L);
        teacher.setUsername("teacher1");
        teacher.setEmail("teacher1@example.com");

        classModuleTeacher = new ClassModuleTeacher();
        classModuleTeacher.setId(1L);
        classModuleTeacher.setClassModuleId(1L);
        classModuleTeacher.setUserId(10L);
        classModuleTeacher.setRole(ClassModuleTeacher.TeacherRole.TEACHER);
        classModuleTeacher.setUser(teacher);
    }

    // --- createClassModulesFromProgram ---

    @Test
    void createClassModulesFromProgram_Success() {
        // Arrange
        List<ProgramModule> programModules = List.of(programModule);
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(programModuleRepository.findByProgramId(1)).thenReturn(programModules);
        when(classModuleRepository.existsByClassIdAndModuleId(1L, 1)).thenReturn(false);
        when(classModuleRepository.save(any(ClassModule.class))).thenAnswer(invocation -> {
            ClassModule cm = invocation.getArgument(0);
            cm.setId(1L);
            return cm;
        });

        // Act
        List<ClassModuleDTO> result = classModuleService.createClassModulesFromProgram(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(classModuleRepository).save(any(ClassModule.class));
    }

    @Test
    void createClassModulesFromProgram_ClassNotFound() {
        // Arrange
        when(classRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModulesFromProgram(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lớp học");
    }

    @Test
    void createClassModulesFromProgram_NoProgramId() {
        // Arrange
        classEntity.setProgramId(null);
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModulesFromProgram(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chưa được gán chương trình đào tạo");
    }

    @Test
    void createClassModulesFromProgram_EmptyProgramModules() {
        // Arrange
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(programModuleRepository.findByProgramId(1)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModulesFromProgram(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chương trình đào tạo chưa có module nào");
    }

    @Test
    void createClassModulesFromProgram_ModuleAlreadyExists() {
        // Arrange
        List<ProgramModule> programModules = List.of(programModule);
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(programModuleRepository.findByProgramId(1)).thenReturn(programModules);
        when(classModuleRepository.existsByClassIdAndModuleId(1L, 1)).thenReturn(true);

        // Act
        List<ClassModuleDTO> result = classModuleService.createClassModulesFromProgram(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // Module already exists, not created
        verify(classModuleRepository, never()).save(any(ClassModule.class));
    }

    @Test
    void createClassModulesFromProgram_MultipleModules_SomeExist() {
        // Arrange
        Module module2 = new Module();
        module2.setId(2);
        ProgramModule programModule2 = new ProgramModule();
        programModule2.setModule(module2);

        List<ProgramModule> programModules = List.of(programModule, programModule2);
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));
        when(programModuleRepository.findByProgramId(1)).thenReturn(programModules);
        when(classModuleRepository.existsByClassIdAndModuleId(1L, 1)).thenReturn(true);
        when(classModuleRepository.existsByClassIdAndModuleId(1L, 2)).thenReturn(false);
        when(classModuleRepository.save(any(ClassModule.class))).thenAnswer(invocation -> {
            ClassModule cm = invocation.getArgument(0);
            cm.setId(2L);
            return cm;
        });

        // Act
        List<ClassModuleDTO> result = classModuleService.createClassModulesFromProgram(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1); // Only module2 is created
        verify(classModuleRepository, times(1)).save(any(ClassModule.class));
    }

    // --- createClassModule ---

    @Test
    void createClassModule_Success() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setClassId(1L);
        dto.setModuleId(1);
        dto.setScheduleType(ClassModule.ScheduleType.fixed);

        when(classRepository.existsById(1L)).thenReturn(true);
        when(moduleRepository.existsById(1)).thenReturn(true);
        when(classModuleRepository.existsByClassIdAndModuleId(1L, 1)).thenReturn(false);
        when(classModuleRepository.save(any(ClassModule.class))).thenAnswer(invocation -> {
            ClassModule cm = invocation.getArgument(0);
            cm.setId(1L);
            return cm;
        });

        // Act
        ClassModuleDTO result = classModuleService.createClassModule(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getClassId()).isEqualTo(1L);
        assertThat(result.getModuleId()).isEqualTo(1);
        verify(classModuleRepository).save(any(ClassModule.class));
    }

    @Test
    void createClassModule_NullClassId() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setClassId(null);
        dto.setModuleId(1);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModule(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("classId và moduleId là bắt buộc");
    }

    @Test
    void createClassModule_NullModuleId() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setClassId(1L);
        dto.setModuleId(null);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModule(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("classId và moduleId là bắt buộc");
    }

    @Test
    void createClassModule_ClassNotFound() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setClassId(999L);
        dto.setModuleId(1);

        when(classRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModule(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lớp học");
    }

    @Test
    void createClassModule_ModuleNotFound() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setClassId(1L);
        dto.setModuleId(999);

        when(classRepository.existsById(1L)).thenReturn(true);
        when(moduleRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModule(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy module");
    }

    @Test
    void createClassModule_AlreadyExists() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setClassId(1L);
        dto.setModuleId(1);

        when(classRepository.existsById(1L)).thenReturn(true);
        when(moduleRepository.existsById(1)).thenReturn(true);
        when(classModuleRepository.existsByClassIdAndModuleId(1L, 1)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.createClassModule(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Module đã được gán vào lớp học này");
    }

    @Test
    void createClassModule_NullScheduleType() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setClassId(1L);
        dto.setModuleId(1);
        dto.setScheduleType(null);

        when(classRepository.existsById(1L)).thenReturn(true);
        when(moduleRepository.existsById(1)).thenReturn(true);
        when(classModuleRepository.existsByClassIdAndModuleId(1L, 1)).thenReturn(false);
        when(classModuleRepository.save(any(ClassModule.class))).thenAnswer(invocation -> {
            ClassModule cm = invocation.getArgument(0);
            cm.setId(1L);
            return cm;
        });

        // Act
        ClassModuleDTO result = classModuleService.createClassModule(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScheduleType()).isEqualTo(ClassModule.ScheduleType.fixed); // Default value
    }

    // --- getClassModulesByClassId ---

    @Test
    void getClassModulesByClassId_Success() {
        // Arrange
        List<ClassModule> classModules = List.of(classModule);
        when(classRepository.existsById(1L)).thenReturn(true);
        when(classModuleRepository.findByClassId(1L)).thenReturn(classModules);

        // Act
        List<ClassModuleDTO> result = classModuleService.getClassModulesByClassId(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassId()).isEqualTo(1L);
    }

    @Test
    void getClassModulesByClassId_ClassNotFound() {
        // Arrange
        when(classRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.getClassModulesByClassId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy lớp học");
    }

    @Test
    void getClassModulesByClassId_Empty() {
        // Arrange
        when(classRepository.existsById(1L)).thenReturn(true);
        when(classModuleRepository.findByClassId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<ClassModuleDTO> result = classModuleService.getClassModulesByClassId(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // --- getClassModuleById ---

    @Test
    void getClassModuleById_Success() {
        // Arrange
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getClassModuleById_NotFound() {
        // Arrange
        when(classModuleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.getClassModuleById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ClassModule");
    }

    // --- updateClassModule ---

    @Test
    void updateClassModule_Success() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setScheduleType(ClassModule.ScheduleType.flexible);

        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));
        when(classModuleRepository.save(any(ClassModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassModuleDTO result = classModuleService.updateClassModule(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getScheduleType()).isEqualTo(ClassModule.ScheduleType.flexible);
        verify(classModuleRepository).save(classModule);
    }

    @Test
    void updateClassModule_NotFound() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        when(classModuleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.updateClassModule(999L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ClassModule");
    }

    @Test
    void updateClassModule_NullScheduleType() {
        // Arrange
        ClassModuleDTO dto = new ClassModuleDTO();
        dto.setScheduleType(null);

        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));
        when(classModuleRepository.save(any(ClassModule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassModuleDTO result = classModuleService.updateClassModule(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        // ScheduleType should remain unchanged
        verify(classModuleRepository).save(classModule);
    }

    // --- deleteClassModule ---

    @Test
    void deleteClassModule_Success() {
        // Arrange
        when(classModuleRepository.existsById(1L)).thenReturn(true);
        doNothing().when(classModuleRepository).deleteById(1L);

        // Act
        classModuleService.deleteClassModule(1L);

        // Assert
        verify(classModuleRepository).deleteById(1L);
    }

    @Test
    void deleteClassModule_NotFound() {
        // Arrange
        when(classModuleRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.deleteClassModule(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ClassModule");
    }

    // --- assignTeacherToClassModule ---

    @Test
    void assignTeacherToClassModule_Success() {
        // Arrange
        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setUserId(10L);
        dto.setRole(ClassModuleTeacher.TeacherRole.TEACHER);

        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(classModuleTeacherRepository.existsByClassModuleIdAndUserId(1L, 10L)).thenReturn(false);
        when(classModuleTeacherRepository.save(any(ClassModuleTeacher.class))).thenAnswer(invocation -> {
            ClassModuleTeacher cmt = invocation.getArgument(0);
            cmt.setId(1L);
            return cmt;
        });

        // Act
        ClassModuleTeacherDTO result = classModuleService.assignTeacherToClassModule(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(10L);
        verify(classModuleTeacherRepository).save(any(ClassModuleTeacher.class));
    }

    @Test
    void assignTeacherToClassModule_ClassModuleNotFound() {
        // Arrange
        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setUserId(10L);

        when(classModuleRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.assignTeacherToClassModule(999L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ClassModule");
    }

    @Test
    void assignTeacherToClassModule_NullUserId() {
        // Arrange
        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setUserId(null);

        when(classModuleRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.assignTeacherToClassModule(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("userId là bắt buộc");
    }

    @Test
    void assignTeacherToClassModule_UserNotFound() {
        // Arrange
        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setUserId(999L);

        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.assignTeacherToClassModule(1L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy người dùng");
    }

    @Test
    void assignTeacherToClassModule_AlreadyAssigned() {
        // Arrange
        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setUserId(10L);

        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(classModuleTeacherRepository.existsByClassModuleIdAndUserId(1L, 10L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.assignTeacherToClassModule(1L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Giáo viên đã được gán vào ClassModule này");
    }

    @Test
    void assignTeacherToClassModule_NullRole() {
        // Arrange
        ClassModuleTeacherDTO dto = new ClassModuleTeacherDTO();
        dto.setUserId(10L);
        dto.setRole(null);

        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(classModuleTeacherRepository.existsByClassModuleIdAndUserId(1L, 10L)).thenReturn(false);
        when(classModuleTeacherRepository.save(any(ClassModuleTeacher.class))).thenAnswer(invocation -> {
            ClassModuleTeacher cmt = invocation.getArgument(0);
            cmt.setId(1L);
            return cmt;
        });

        // Act
        ClassModuleTeacherDTO result = classModuleService.assignTeacherToClassModule(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(ClassModuleTeacher.TeacherRole.TEACHER); // Default role
    }

    // --- getClassModuleTeachers ---

    @Test
    void getClassModuleTeachers_Success() {
        // Arrange
        List<ClassModuleTeacher> teachers = List.of(classModuleTeacher);
        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(classModuleTeacherRepository.findByClassModuleId(1L)).thenReturn(teachers);

        // Act
        List<ClassModuleTeacherDTO> result = classModuleService.getClassModuleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
    }

    @Test
    void getClassModuleTeachers_ClassModuleNotFound() {
        // Arrange
        when(classModuleRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.getClassModuleTeachers(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy ClassModule");
    }

    @Test
    void getClassModuleTeachers_Empty() {
        // Arrange
        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(classModuleTeacherRepository.findByClassModuleId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<ClassModuleTeacherDTO> result = classModuleService.getClassModuleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    // --- removeTeacherFromClassModule ---

    @Test
    void removeTeacherFromClassModule_Success() {
        // Arrange
        when(classModuleTeacherRepository.findByClassModuleIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(classModuleTeacher));
        doNothing().when(classModuleTeacherRepository).delete(classModuleTeacher);

        // Act
        classModuleService.removeTeacherFromClassModule(1L, 10L);

        // Assert
        verify(classModuleTeacherRepository).delete(classModuleTeacher);
    }

    @Test
    void removeTeacherFromClassModule_NotFound() {
        // Arrange
        when(classModuleTeacherRepository.findByClassModuleIdAndUserId(1L, 10L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.removeTeacherFromClassModule(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy giáo viên");
    }

    // --- updateTeacherRole ---

    @Test
    void updateTeacherRole_Success() {
        // Arrange
        ClassModuleTeacher.TeacherRole newRole = ClassModuleTeacher.TeacherRole.TEACHER;
        when(classModuleTeacherRepository.findByClassModuleIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(classModuleTeacher));
        when(classModuleTeacherRepository.save(any(ClassModuleTeacher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassModuleTeacherDTO result = classModuleService.updateTeacherRole(1L, 10L, newRole);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(newRole);
        verify(classModuleTeacherRepository).save(classModuleTeacher);
    }

    @Test
    void updateTeacherRole_NotFound() {
        // Arrange
        when(classModuleTeacherRepository.findByClassModuleIdAndUserId(1L, 10L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> classModuleService.updateTeacherRole(1L, 10L, ClassModuleTeacher.TeacherRole.TEACHER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy giáo viên");
    }

    // --- convertToDTO tests ---

    @Test
    void convertToDTO_WithClassEntity() {
        // Arrange
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getClassName()).isEqualTo("Test Class");
    }

    @Test
    void convertToDTO_WithoutClassEntity_LoadFromRepository() {
        // Arrange
        classModule.setClassEntity(null);
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));
        when(classRepository.findById(1L)).thenReturn(Optional.of(classEntity));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getClassName()).isEqualTo("Test Class");
        verify(classRepository).findById(1L);
    }

    @Test
    void convertToDTO_WithModule() {
        // Arrange
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModuleName()).isEqualTo("Test Module");
    }

    @Test
    void convertToDTO_WithoutModule_LoadFromRepository() {
        // Arrange
        classModule.setModule(null);
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));
        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModuleName()).isEqualTo("Test Module");
        verify(moduleRepository).findById(1);
    }

    @Test
    void convertToDTO_WithTeachers() {
        // Arrange
        List<ClassModuleTeacher> teachers = List.of(classModuleTeacher);
        classModule.setTeachers(teachers);
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTeachers()).isNotNull();
        assertThat(result.getTeachers()).hasSize(1);
    }

    @Test
    void convertToDTO_WithoutTeachers() {
        // Arrange
        classModule.setTeachers(null);
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTeachers()).isNull();
    }

    @Test
    void convertToDTO_WithEmptyTeachers() {
        // Arrange
        classModule.setTeachers(Collections.emptyList());
        when(classModuleRepository.findById(1L)).thenReturn(Optional.of(classModule));

        // Act
        ClassModuleDTO result = classModuleService.getClassModuleById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTeachers()).isNull(); // Empty list is not set
    }

    // --- convertTeacherToDTO tests ---

    @Test
    void convertTeacherToDTO_WithUser() {
        // Arrange
        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(classModuleTeacherRepository.findByClassModuleId(1L)).thenReturn(List.of(classModuleTeacher));

        // Act
        List<ClassModuleTeacherDTO> result = classModuleService.getClassModuleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isEqualTo("teacher1");
        assertThat(result.get(0).getUserEmail()).isEqualTo("teacher1@example.com");
    }

    @Test
    void convertTeacherToDTO_WithoutUser_LoadFromRepository() {
        // Arrange
        classModuleTeacher.setUser(null);
        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(classModuleTeacherRepository.findByClassModuleId(1L)).thenReturn(List.of(classModuleTeacher));
        when(userRepository.findById(10L)).thenReturn(Optional.of(teacher));

        // Act
        List<ClassModuleTeacherDTO> result = classModuleService.getClassModuleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isEqualTo("teacher1");
        verify(userRepository).findById(10L);
    }

    @Test
    void convertTeacherToDTO_UserNotFound() {
        // Arrange
        classModuleTeacher.setUser(null);
        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(classModuleTeacherRepository.findByClassModuleId(1L)).thenReturn(List.of(classModuleTeacher));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        // Act
        List<ClassModuleTeacherDTO> result = classModuleService.getClassModuleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isNull();
    }

    @Test
    void convertTeacherToDTO_NullUserId() {
        // Arrange
        classModuleTeacher.setUser(null);
        classModuleTeacher.setUserId(null);
        when(classModuleRepository.existsById(1L)).thenReturn(true);
        when(classModuleTeacherRepository.findByClassModuleId(1L)).thenReturn(List.of(classModuleTeacher));

        // Act
        List<ClassModuleTeacherDTO> result = classModuleService.getClassModuleTeachers(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isNull();
        verify(userRepository, never()).findById(anyLong());
    }
}


