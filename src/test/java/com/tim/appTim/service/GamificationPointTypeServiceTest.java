package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.GamificationPointType;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.GamificationPointTypeRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationPointTypeServiceTest {

    @Mock
    private GamificationPointTypeRepository pointTypeRepository;

    @InjectMocks
    private GamificationPointTypeService pointTypeService;

    private GamificationPointType pointType;
    private GamificationPointTypeDTO pointTypeDTO;

    @BeforeEach
    void setUp() {
        pointType = new GamificationPointType();
        pointType.setId(1);
        pointType.setName("Diligence Points");
        pointType.setDescription("Points for diligence");
        pointType.setMaxPoints(1000);
        pointType.setImageUrl("https://example.com/image.jpg");
        pointType.setIsActive(true);
        pointType.setShowOnDashboard(true);
        pointType.setCreatedBy(1);
        pointType.setCreatedAt(LocalDateTime.now());

        pointTypeDTO = new GamificationPointTypeDTO();
        pointTypeDTO.setName("Diligence Points");
        pointTypeDTO.setDescription("Points for diligence");
        pointTypeDTO.setMaxPoints(1000);
        pointTypeDTO.setImageUrl("https://example.com/image.jpg");
        pointTypeDTO.setIsActive(true);
        pointTypeDTO.setShowOnDashboard(true);
        pointTypeDTO.setCreatedBy(1);
    }

    @Test
    void getAllActivePointTypes_ShouldReturnActivePointTypes() {
        // Arrange
        List<GamificationPointType> activeTypes = List.of(pointType);
        when(pointTypeRepository.findByIsActiveTrue()).thenReturn(activeTypes);

        // Act
        List<GamificationPointTypeDTO> result = pointTypeService.getAllActivePointTypes();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(pointType.getName(), result.get(0).getName());
        verify(pointTypeRepository).findByIsActiveTrue();
    }

    @Test
    void getAllActivePointTypes_WhenNoActiveTypes_ShouldReturnEmptyList() {
        // Arrange
        when(pointTypeRepository.findByIsActiveTrue()).thenReturn(new ArrayList<>());

        // Act
        List<GamificationPointTypeDTO> result = pointTypeService.getAllActivePointTypes();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pointTypeRepository).findByIsActiveTrue();
    }

    @Test
    void getDashboardPointTypes_ShouldReturnDashboardPointTypes() {
        // Arrange
        List<GamificationPointType> dashboardTypes = List.of(pointType);
        when(pointTypeRepository.findByShowOnDashboardTrueAndIsActiveTrue()).thenReturn(dashboardTypes);

        // Act
        List<GamificationPointTypeDTO> result = pointTypeService.getDashboardPointTypes();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(pointTypeRepository).findByShowOnDashboardTrueAndIsActiveTrue();
    }

    @Test
    void getPointTypeById_WhenExists_ShouldReturnPointType() {
        // Arrange
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(pointType));

        // Act
        GamificationPointTypeDTO result = pointTypeService.getPointTypeById(1);

        // Assert
        assertNotNull(result);
        assertEquals(pointType.getName(), result.getName());
        assertEquals(pointType.getDescription(), result.getDescription());
        verify(pointTypeRepository).findById(1);
    }

    @Test
    void getPointTypeById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(pointTypeRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            pointTypeService.getPointTypeById(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy loại điểm"));
        verify(pointTypeRepository).findById(999);
    }

    @Test
    void createPointType_ShouldCreateAndReturnPointType() {
        // Arrange
        when(pointTypeRepository.save(any(GamificationPointType.class))).thenAnswer(invocation -> {
            GamificationPointType saved = invocation.getArgument(0);
            saved.setId(1);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        GamificationPointTypeDTO result = pointTypeService.createPointType(pointTypeDTO);

        // Assert
        assertNotNull(result);
        assertEquals(pointTypeDTO.getName(), result.getName());
        assertEquals(pointTypeDTO.getDescription(), result.getDescription());
        assertTrue(result.getIsActive()); // Default to true
        assertTrue(result.getShowOnDashboard()); // Default to true
        verify(pointTypeRepository).save(any(GamificationPointType.class));
    }

    @Test
    void createPointType_WithNullIsActive_ShouldDefaultToTrue() {
        // Arrange
        pointTypeDTO.setIsActive(null);
        when(pointTypeRepository.save(any(GamificationPointType.class))).thenAnswer(invocation -> {
            GamificationPointType saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        GamificationPointTypeDTO result = pointTypeService.createPointType(pointTypeDTO);

        // Assert
        assertNotNull(result);
        assertTrue(result.getIsActive());
        verify(pointTypeRepository).save(any(GamificationPointType.class));
    }

    @Test
    void createPointType_WithNullShowOnDashboard_ShouldDefaultToTrue() {
        // Arrange
        pointTypeDTO.setShowOnDashboard(null);
        when(pointTypeRepository.save(any(GamificationPointType.class))).thenAnswer(invocation -> {
            GamificationPointType saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        GamificationPointTypeDTO result = pointTypeService.createPointType(pointTypeDTO);

        // Assert
        assertNotNull(result);
        assertTrue(result.getShowOnDashboard());
        verify(pointTypeRepository).save(any(GamificationPointType.class));
    }

    @Test
    void updatePointType_WhenExists_ShouldUpdateAndReturnPointType() {
        // Arrange
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(pointType));
        when(pointTypeRepository.save(any(GamificationPointType.class))).thenReturn(pointType);

        GamificationPointTypeDTO updateDTO = new GamificationPointTypeDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setDescription("Updated Description");

        // Act
        GamificationPointTypeDTO result = pointTypeService.updatePointType(1, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", pointType.getName());
        assertEquals("Updated Description", pointType.getDescription());
        verify(pointTypeRepository).findById(1);
        verify(pointTypeRepository).save(pointType);
    }

    @Test
    void updatePointType_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(pointTypeRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            pointTypeService.updatePointType(999, pointTypeDTO);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy loại điểm"));
        verify(pointTypeRepository).findById(999);
        verify(pointTypeRepository, never()).save(any());
    }

    @Test
    void updatePointType_WithPartialUpdate_ShouldUpdateOnlyProvidedFields() {
        // Arrange
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(pointType));
        when(pointTypeRepository.save(any(GamificationPointType.class))).thenReturn(pointType);

        GamificationPointTypeDTO updateDTO = new GamificationPointTypeDTO();
        updateDTO.setName("Updated Name");
        // Other fields are null

        // Act
        pointTypeService.updatePointType(1, updateDTO);

        // Assert
        assertEquals("Updated Name", pointType.getName());
        // Other fields should remain unchanged
        assertNotNull(pointType.getDescription());
        verify(pointTypeRepository).save(pointType);
    }

    @Test
    void updatePointType_WithNullFields_ShouldNotUpdate() {
        // Arrange
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(pointType));
        when(pointTypeRepository.save(any(GamificationPointType.class))).thenReturn(pointType);

        GamificationPointTypeDTO updateDTO = new GamificationPointTypeDTO();
        // All fields are null

        // Act
        pointTypeService.updatePointType(1, updateDTO);

        // Assert
        // Original values should remain
        assertNotNull(pointType.getName());
        verify(pointTypeRepository).save(pointType);
    }

    @Test
    void deletePointType_WhenExists_ShouldDelete() {
        // Arrange
        when(pointTypeRepository.existsById(1)).thenReturn(true);
        doNothing().when(pointTypeRepository).deleteById(1);

        // Act
        pointTypeService.deletePointType(1);

        // Assert
        verify(pointTypeRepository).existsById(1);
        verify(pointTypeRepository).deleteById(1);
    }

    @Test
    void deletePointType_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(pointTypeRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            pointTypeService.deletePointType(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy loại điểm"));
        verify(pointTypeRepository).existsById(999);
        verify(pointTypeRepository, never()).deleteById(anyInt());
    }

    @Test
    void mapToDTO_ShouldMapAllFields() {
        // Arrange
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(pointType));

        // Act
        GamificationPointTypeDTO result = pointTypeService.getPointTypeById(1);

        // Assert
        assertNotNull(result);
        assertEquals(pointType.getId(), result.getId());
        assertEquals(pointType.getName(), result.getName());
        assertEquals(pointType.getDescription(), result.getDescription());
        assertEquals(pointType.getMaxPoints(), result.getMaxPoints());
        assertEquals(pointType.getImageUrl(), result.getImageUrl());
        assertEquals(pointType.getIsActive(), result.getIsActive());
        assertEquals(pointType.getShowOnDashboard(), result.getShowOnDashboard());
        assertEquals(pointType.getCreatedBy(), result.getCreatedBy());
        assertEquals(pointType.getCreatedAt(), result.getCreatedAt());
    }
}


