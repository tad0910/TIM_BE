package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.Module;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ModuleSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleServiceImplTest {

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private ModuleSessionRepository moduleSessionRepository;

    @InjectMocks
    private ModuleServiceImpl moduleService;

    private Module module;
    private ModuleSession moduleSession;
    private ModuleDTO moduleDTO;
    private ModuleSessionDTO moduleSessionDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        module = new Module();
        module.setId(1);
        module.setName("Test Module");
        module.setDescription("Test Description");

        moduleSession = new ModuleSession();
        moduleSession.setId(1L);
        moduleSession.setModuleId(1);
        moduleSession.setSessionNumber(1);
        moduleSession.setTitle("Session 1");
        moduleSession.setContent("Session Content");

        moduleDTO = new ModuleDTO();
        moduleDTO.setId(1);
        moduleDTO.setName("Test Module");
        moduleDTO.setDescription("Test Description");

        moduleSessionDTO = new ModuleSessionDTO();
        moduleSessionDTO.setId(1L);
        moduleSessionDTO.setModuleId(1);
        moduleSessionDTO.setSessionNumber(1);
        moduleSessionDTO.setTitle("Session 1");
        moduleSessionDTO.setContent("Session Content");
    }

    // --- getAllModules ---

    @Test
    void getAllModules_WithData_ShouldReturnPage() {
        // Arrange
        Page<Module> modulePage = new PageImpl<>(List.of(module));
        when(moduleRepository.findAll(pageable)).thenReturn(modulePage);
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        Page<ModuleDTO> result = moduleService.getAllModules(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Module");
        verify(moduleRepository).findAll(pageable);
    }

    @Test
    void getAllModules_EmptyPage_ShouldReturnEmptyPage() {
        // Arrange
        Page<Module> emptyPage = new PageImpl<>(Collections.emptyList());
        when(moduleRepository.findAll(pageable)).thenReturn(emptyPage);

        // Act
        Page<ModuleDTO> result = moduleService.getAllModules(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(moduleRepository).findAll(pageable);
    }

    @Test
    void getAllModules_WithSessions_ShouldIncludeSessions() {
        // Arrange
        Page<Module> modulePage = new PageImpl<>(List.of(module));
        when(moduleRepository.findAll(pageable)).thenReturn(modulePage);
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(List.of(moduleSession));

        // Act
        Page<ModuleDTO> result = moduleService.getAllModules(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSessions()).hasSize(1);
        assertThat(result.getContent().get(0).getSessions().get(0).getTitle())
                .isEqualTo("Session 1");
        verify(moduleSessionRepository).findByModuleIdOrderBySessionNumberAsc(1);
    }

    // --- getModuleById ---

    @Test
    void getModuleById_WhenExists_ShouldReturnDTO() {
        // Arrange
        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(List.of(moduleSession));

        // Act
        ModuleDTO result = moduleService.getModuleById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("Test Module");
        assertThat(result.getSessions()).hasSize(1);
        verify(moduleRepository).findById(1);
    }

    @Test
    void getModuleById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(moduleRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> moduleService.getModuleById(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Module not found with ID: 999");
        verify(moduleRepository).findById(999);
    }

    @Test
    void getModuleById_WithoutSessions_ShouldReturnEmptySessionsList() {
        // Arrange
        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        ModuleDTO result = moduleService.getModuleById(1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessions()).isEmpty();
    }

    // --- searchModulesByName ---

    @Test
    void searchModulesByName_WhenFound_ShouldReturnPage() {
        // Arrange
        Page<Module> modulePage = new PageImpl<>(List.of(module));
        when(moduleRepository.searchByName("test", pageable)).thenReturn(modulePage);
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        Page<ModuleDTO> result = moduleService.searchModulesByName("test", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Module");
        verify(moduleRepository).searchByName("test", pageable);
    }

    @Test
    void searchModulesByName_WhenEmpty_ShouldThrowException() {
        // Arrange
        Page<Module> emptyPage = new PageImpl<>(Collections.emptyList());
        when(moduleRepository.searchByName("nonexistent", pageable)).thenReturn(emptyPage);

        // Act & Assert
        assertThatThrownBy(() -> moduleService.searchModulesByName("nonexistent", pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy module nào với từ khóa: nonexistent");
        verify(moduleRepository).searchByName("nonexistent", pageable);
    }

    @Test
    void searchModulesByName_WithMultipleResults_ShouldReturnAll() {
        // Arrange
        Module module2 = new Module();
        module2.setId(2);
        module2.setName("Another Test Module");
        module2.setDescription("Another Description");

        Page<Module> modulePage = new PageImpl<>(List.of(module, module2));
        when(moduleRepository.searchByName("test", pageable)).thenReturn(modulePage);
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(2))
                .thenReturn(Collections.emptyList());

        // Act
        Page<ModuleDTO> result = moduleService.searchModulesByName("test", pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
    }

    // --- createModule ---

    @Test
    void createModule_WithoutSessions_ShouldCreateModule() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("New Module");
        dto.setDescription("New Description");
        dto.setSessions(null);

        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            m.setId(1);
            return m;
        });
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        ModuleDTO result = moduleService.createModule(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("New Module");
        assertThat(result.getSessions()).isEmpty();
        verify(moduleRepository).save(any(Module.class));
        verify(moduleSessionRepository, never()).saveAll(any());
    }

    @Test
    void createModule_WithSessions_ShouldCreateModuleAndSessions() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("New Module");
        dto.setDescription("New Description");

        ModuleSessionDTO session1 = new ModuleSessionDTO();
        session1.setSessionNumber(1);
        session1.setTitle("Session 1");
        session1.setContent("Content 1");

        ModuleSessionDTO session2 = new ModuleSessionDTO();
        session2.setSessionNumber(2);
        session2.setTitle("Session 2");
        session2.setContent("Content 2");

        dto.setSessions(List.of(session1, session2));

        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            m.setId(1);
            return m;
        });
        when(moduleSessionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ModuleSession> sessions = invocation.getArgument(0);
            sessions.get(0).setId(1L);
            sessions.get(1).setId(2L);
            return sessions;
        });

        ModuleSession savedSession1 = new ModuleSession();
        savedSession1.setId(1L);
        savedSession1.setModuleId(1);
        savedSession1.setSessionNumber(1);
        savedSession1.setTitle("Session 1");
        savedSession1.setContent("Content 1");

        ModuleSession savedSession2 = new ModuleSession();
        savedSession2.setId(2L);
        savedSession2.setModuleId(1);
        savedSession2.setSessionNumber(2);
        savedSession2.setTitle("Session 2");
        savedSession2.setContent("Content 2");

        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(List.of(savedSession1, savedSession2));

        // Act
        ModuleDTO result = moduleService.createModule(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getSessions()).hasSize(2);
        verify(moduleRepository).save(any(Module.class));
        verify(moduleSessionRepository).saveAll(anyList());
    }

    @Test
    void createModule_WithEmptySessionsList_ShouldNotSaveSessions() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("New Module");
        dto.setDescription("New Description");
        dto.setSessions(Collections.emptyList());

        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            m.setId(1);
            return m;
        });
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        ModuleDTO result = moduleService.createModule(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessions()).isEmpty();
        verify(moduleSessionRepository, never()).saveAll(any());
    }

    // --- updateModule ---

    @Test
    void updateModule_WhenExists_ShouldUpdateModule() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("Updated Module");
        dto.setDescription("Updated Description");

        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            return m;
        });
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        ModuleDTO result = moduleService.updateModule(1, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Module");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        verify(moduleRepository).findById(1);
        verify(moduleRepository).save(any(Module.class));
    }

    @Test
    void updateModule_WhenNotExists_ShouldThrowException() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("Updated Module");
        dto.setDescription("Updated Description");

        when(moduleRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> moduleService.updateModule(999, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Module not found with ID: 999");
        verify(moduleRepository).findById(999);
        verify(moduleRepository, never()).save(any(Module.class));
    }

    @Test
    void updateModule_WithSessions_ShouldPreserveSessions() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("Updated Module");
        dto.setDescription("Updated Description");

        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            return m;
        });
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(List.of(moduleSession));

        // Act
        ModuleDTO result = moduleService.updateModule(1, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSessions()).hasSize(1);
    }

    // --- deleteModule ---

    @Test
    void deleteModule_WhenExists_ShouldDeleteModule() {
        // Arrange
        when(moduleRepository.existsById(1)).thenReturn(true);
        doNothing().when(moduleRepository).deleteById(1);

        // Act
        moduleService.deleteModule(1);

        // Assert
        verify(moduleRepository).existsById(1);
        verify(moduleRepository).deleteById(1);
    }

    @Test
    void deleteModule_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(moduleRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> moduleService.deleteModule(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Module not found with ID: 999");
        verify(moduleRepository).existsById(999);
        verify(moduleRepository, never()).deleteById(anyInt());
    }

    // --- toDTO (private method tested through public methods) ---

    @Test
    void toDTO_WithAllFields_ShouldMapCorrectly() {
        // Arrange
        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(List.of(moduleSession));

        // Act
        ModuleDTO result = moduleService.getModuleById(1);

        // Assert
        assertThat(result.getId()).isEqualTo(module.getId());
        assertThat(result.getName()).isEqualTo(module.getName());
        assertThat(result.getDescription()).isEqualTo(module.getDescription());
        assertThat(result.getSessions()).hasSize(1);
        assertThat(result.getSessions().get(0).getId()).isEqualTo(moduleSession.getId());
        assertThat(result.getSessions().get(0).getModuleId()).isEqualTo(moduleSession.getModuleId());
        assertThat(result.getSessions().get(0).getSessionNumber())
                .isEqualTo(moduleSession.getSessionNumber());
        assertThat(result.getSessions().get(0).getTitle()).isEqualTo(moduleSession.getTitle());
        assertThat(result.getSessions().get(0).getContent()).isEqualTo(moduleSession.getContent());
    }

    @Test
    void toDTO_WithMultipleSessions_ShouldOrderBySessionNumber() {
        // Arrange
        ModuleSession session1 = new ModuleSession();
        session1.setId(1L);
        session1.setModuleId(1);
        session1.setSessionNumber(2);
        session1.setTitle("Session 2");

        ModuleSession session2 = new ModuleSession();
        session2.setId(2L);
        session2.setModuleId(1);
        session2.setSessionNumber(1);
        session2.setTitle("Session 1");

        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(List.of(session2, session1)); // Repository returns ordered

        // Act
        ModuleDTO result = moduleService.getModuleById(1);

        // Assert
        assertThat(result.getSessions()).hasSize(2);
        // Should be ordered by sessionNumber (1, 2)
        assertThat(result.getSessions().get(0).getSessionNumber()).isEqualTo(1);
        assertThat(result.getSessions().get(1).getSessionNumber()).isEqualTo(2);
    }

    @Test
    void createModule_WithNullDescription_ShouldHandleGracefully() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("New Module");
        dto.setDescription(null);
        dto.setSessions(null);

        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            m.setId(1);
            return m;
        });
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        ModuleDTO result = moduleService.createModule(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
    }

    @Test
    void updateModule_WithNullDescription_ShouldHandleGracefully() {
        // Arrange
        ModuleDTO dto = new ModuleDTO();
        dto.setName("Updated Module");
        dto.setDescription(null);

        when(moduleRepository.findById(1)).thenReturn(Optional.of(module));
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            return m;
        });
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());

        // Act
        ModuleDTO result = moduleService.updateModule(1, dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
    }

    @Test
    void getAllModules_WithMultipleModules_ShouldReturnAll() {
        // Arrange
        Module module2 = new Module();
        module2.setId(2);
        module2.setName("Module 2");
        module2.setDescription("Description 2");

        Page<Module> modulePage = new PageImpl<>(List.of(module, module2));
        when(moduleRepository.findAll(pageable)).thenReturn(modulePage);
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(1))
                .thenReturn(Collections.emptyList());
        when(moduleSessionRepository.findByModuleIdOrderBySessionNumberAsc(2))
                .thenReturn(Collections.emptyList());

        // Act
        Page<ModuleDTO> result = moduleService.getAllModules(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1);
        assertThat(result.getContent().get(1).getId()).isEqualTo(2);
    }
}


