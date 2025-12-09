package com.tim.appTim.service;

import com.tim.appTim.dto.GamificationBehaviorDTO;
import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
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
class GamificationBehaviorServiceTest {

    @Mock
    private GamificationBehaviorRepository behaviorRepository;

    @Mock
    private GamificationBehaviorGroupRepository groupRepository;

    @InjectMocks
    private GamificationBehaviorService behaviorService;

    private GamificationBehavior behavior;
    private GamificationBehaviorGroup group;
    private GamificationBehaviorDTO behaviorDTO;

    @BeforeEach
    void setUp() {
        group = new GamificationBehaviorGroup();
        group.setId(1);
        group.setName("Học tập");

        behavior = new GamificationBehavior();
        behavior.setId(1);
        behavior.setName("Điểm danh đúng giờ");
        behavior.setGroup(group);
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.DAILY);
        behavior.setMaxTimesPerFrequency(1);
        behavior.setPointDiligence(10);
        behavior.setPointCompetence(0);
        behavior.setPointExperience(5);
        behavior.setCreatedAt(LocalDateTime.now());

        behaviorDTO = new GamificationBehaviorDTO();
        behaviorDTO.setGroupId(1);
        behaviorDTO.setName("Điểm danh đúng giờ");
        behaviorDTO.setFrequencyType("DAILY");
        behaviorDTO.setMaxTimesPerFrequency(1);
        behaviorDTO.setPointDiligence(10);
        behaviorDTO.setPointCompetence(0);
        behaviorDTO.setPointExperience(5);
    }

    @Test
    void getAllBehaviors_ShouldReturnAllBehaviors() {
        // Arrange
        List<GamificationBehavior> behaviors = List.of(behavior);
        when(behaviorRepository.findAll()).thenReturn(behaviors);

        // Act
        List<GamificationBehaviorDTO> result = behaviorService.getAllBehaviors();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(behavior.getName(), result.get(0).getName());
        verify(behaviorRepository).findAll();
    }

    @Test
    void getAllBehaviors_WhenNoBehaviors_ShouldReturnEmptyList() {
        // Arrange
        when(behaviorRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<GamificationBehaviorDTO> result = behaviorService.getAllBehaviors();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(behaviorRepository).findAll();
    }

    @Test
    void getBehaviorById_WhenExists_ShouldReturnBehavior() {
        // Arrange
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));

        // Act
        GamificationBehaviorDTO result = behaviorService.getBehaviorById(1);

        // Assert
        assertNotNull(result);
        assertEquals(behavior.getName(), result.getName());
        assertEquals(behavior.getName(), result.getName());
        verify(behaviorRepository).findById(1);
    }

    @Test
    void getBehaviorById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(behaviorRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            behaviorService.getBehaviorById(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy hành vi"));
        verify(behaviorRepository).findById(999);
    }

    @Test
    void getBehaviorByName_WhenExists_ShouldReturnBehavior() {
        // Arrange
        when(behaviorRepository.findByName("Điểm danh đúng giờ")).thenReturn(Optional.of(behavior));

        // Act
        GamificationBehaviorDTO result = behaviorService.getBehaviorByName("Điểm danh đúng giờ");

        // Assert
        assertNotNull(result);
        assertEquals(behavior.getName(), result.getName());
        verify(behaviorRepository).findByName("Điểm danh đúng giờ");
    }

    @Test
    void getBehaviorByName_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(behaviorRepository.findByName("INVALID_NAME")).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            behaviorService.getBehaviorByName("INVALID_NAME");
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy hành vi"));
        verify(behaviorRepository).findByName("INVALID_NAME");
    }

    @Test
    void createBehavior_ShouldCreateAndReturnBehavior() {
        // Arrange
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(behaviorRepository.save(any(GamificationBehavior.class))).thenAnswer(invocation -> {
            GamificationBehavior saved = invocation.getArgument(0);
            saved.setId(1);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        GamificationBehaviorDTO result = behaviorService.createBehavior(behaviorDTO);

        // Assert
        assertNotNull(result);
        assertEquals(behaviorDTO.getName(), result.getName());
        assertEquals(behaviorDTO.getName(), result.getName());
        verify(groupRepository).findById(1);
        verify(behaviorRepository).save(any(GamificationBehavior.class));
    }

    @Test
    void createBehavior_WhenGroupNotExists_ShouldThrowException() {
        // Arrange
        when(groupRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            behaviorDTO.setGroupId(999);
            behaviorService.createBehavior(behaviorDTO);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy nhóm hành vi"));
        verify(groupRepository).findById(999);
        verify(behaviorRepository, never()).save(any());
    }

    @Test
    void updateBehavior_WhenExists_ShouldUpdateAndReturnBehavior() {
        // Arrange
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(behaviorRepository.save(any(GamificationBehavior.class))).thenReturn(behavior);

        GamificationBehaviorDTO updateDTO = new GamificationBehaviorDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setPointDiligence(20);

        // Act
        GamificationBehaviorDTO result = behaviorService.updateBehavior(1, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", behavior.getName());
        assertEquals(20, behavior.getPointDiligence());
        verify(behaviorRepository).findById(1);
        verify(behaviorRepository).save(behavior);
    }

    @Test
    void updateBehavior_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(behaviorRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            behaviorService.updateBehavior(999, behaviorDTO);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy hành vi"));
        verify(behaviorRepository).findById(999);
        verify(behaviorRepository, never()).save(any());
    }

    @Test
    void updateBehavior_WithNewGroup_ShouldUpdateGroup() {
        // Arrange
        GamificationBehaviorGroup newGroup = new GamificationBehaviorGroup();
        newGroup.setId(2);
        newGroup.setName("New Group");

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(groupRepository.findById(2)).thenReturn(Optional.of(newGroup));
        when(behaviorRepository.save(any(GamificationBehavior.class))).thenReturn(behavior);

        GamificationBehaviorDTO updateDTO = new GamificationBehaviorDTO();
        updateDTO.setGroupId(2);

        // Act
        behaviorService.updateBehavior(1, updateDTO);

        // Assert
        assertEquals(newGroup, behavior.getGroup());
        verify(groupRepository).findById(2);
        verify(behaviorRepository).save(behavior);
    }

    @Test
    void updateBehavior_WithNullFields_ShouldNotUpdate() {
        // Arrange
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(behaviorRepository.save(any(GamificationBehavior.class))).thenReturn(behavior);

        GamificationBehaviorDTO updateDTO = new GamificationBehaviorDTO();
        // All fields are null

        // Act
        behaviorService.updateBehavior(1, updateDTO);

        // Assert
        // Original values should remain
        assertNotNull(behavior.getName());
        verify(behaviorRepository).save(behavior);
    }

    @Test
    void updateBehavior_WithFrequencyType_ShouldUpdate() {
        // Arrange
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(behaviorRepository.save(any(GamificationBehavior.class))).thenReturn(behavior);

        GamificationBehaviorDTO updateDTO = new GamificationBehaviorDTO();
        updateDTO.setFrequencyType("WEEKLY");

        // Act
        behaviorService.updateBehavior(1, updateDTO);

        // Assert
        assertEquals(GamificationBehavior.FrequencyType.WEEKLY, behavior.getFrequencyType());
        verify(behaviorRepository).save(behavior);
    }

    @Test
    void deleteBehavior_WhenExists_ShouldDelete() {
        // Arrange
        when(behaviorRepository.existsById(1)).thenReturn(true);
        doNothing().when(behaviorRepository).deleteById(1);

        // Act
        behaviorService.deleteBehavior(1);

        // Assert
        verify(behaviorRepository).existsById(1);
        verify(behaviorRepository).deleteById(1);
    }

    @Test
    void deleteBehavior_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(behaviorRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            behaviorService.deleteBehavior(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy hành vi"));
        verify(behaviorRepository).existsById(999);
        verify(behaviorRepository, never()).deleteById(anyInt());
    }

    @Test
    void mapToDTO_ShouldMapAllFields() {
        // Arrange
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));

        // Act
        GamificationBehaviorDTO result = behaviorService.getBehaviorById(1);

        // Assert
        assertNotNull(result);
        assertEquals(behavior.getId(), result.getId());
        assertEquals(behavior.getName(), result.getName());
        assertEquals(behavior.getFrequencyType().name(), result.getFrequencyType());
        assertEquals(behavior.getMaxTimesPerFrequency(), result.getMaxTimesPerFrequency());
        assertEquals(behavior.getPointDiligence(), result.getPointDiligence());
        assertEquals(behavior.getPointCompetence(), result.getPointCompetence());
        assertEquals(behavior.getPointExperience(), result.getPointExperience());
        assertEquals(behavior.getCreatedAt(), result.getCreatedAt());
        assertEquals(group.getId(), result.getGroupId());
        assertEquals(group.getName(), result.getGroupName());
    }

    @Test
    void mapToDTO_WhenGroupIsNull_ShouldHandleGracefully() {
        // Arrange
        behavior.setGroup(null);
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));

        // Act
        GamificationBehaviorDTO result = behaviorService.getBehaviorById(1);

        // Assert
        assertNotNull(result);
        assertNull(result.getGroupId());
        assertNull(result.getGroupName());
    }
}

