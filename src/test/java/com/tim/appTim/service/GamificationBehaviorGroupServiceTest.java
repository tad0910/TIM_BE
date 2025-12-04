package com.tim.appTim.service;

import com.tim.appTim.dto.GamificationBehaviorDTO;
import com.tim.appTim.dto.GamificationBehaviorGroupDTO;
import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
import com.tim.appTim.repository.GamificationBehaviorRepository;
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
class GamificationBehaviorGroupServiceTest {

    @Mock
    private GamificationBehaviorGroupRepository groupRepository;

    @Mock
    private GamificationBehaviorRepository behaviorRepository;

    @InjectMocks
    private GamificationBehaviorGroupService groupService;

    private GamificationBehaviorGroup group;
    private GamificationBehaviorGroupDTO groupDTO;
    private GamificationBehavior behavior;

    @BeforeEach
    void setUp() {
        group = new GamificationBehaviorGroup();
        group.setId(1);
        group.setName("Học tập");
        group.setCreatedAt(LocalDateTime.now());

        groupDTO = new GamificationBehaviorGroupDTO();
        groupDTO.setName("Học tập");

        behavior = new GamificationBehavior();
        behavior.setId(1);
        behavior.setCode("ATTEND_ON_TIME");
        behavior.setName("Điểm danh đúng giờ");
        behavior.setGroup(group);
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.DAILY);
        behavior.setMaxTimesPerFrequency(1);
        behavior.setPointDiligence(10);
        behavior.setPointCompetence(0);
        behavior.setPointExperience(5);
        behavior.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getAllGroups_ShouldReturnAllGroups() {
        // Arrange
        List<GamificationBehaviorGroup> groups = List.of(group);
        when(groupRepository.findAll()).thenReturn(groups);
        when(behaviorRepository.findByGroupId(1)).thenReturn(List.of(behavior));

        // Act
        List<GamificationBehaviorGroupDTO> result = groupService.getAllGroups();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(group.getName(), result.get(0).getName());
        verify(groupRepository).findAll();
        verify(behaviorRepository).findByGroupId(1);
    }

    @Test
    void getAllGroups_WhenNoGroups_ShouldReturnEmptyList() {
        // Arrange
        when(groupRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<GamificationBehaviorGroupDTO> result = groupService.getAllGroups();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(groupRepository).findAll();
    }

    @Test
    void getAllGroups_ShouldIncludeBehaviors() {
        // Arrange
        List<GamificationBehaviorGroup> groups = List.of(group);
        when(groupRepository.findAll()).thenReturn(groups);
        when(behaviorRepository.findByGroupId(1)).thenReturn(List.of(behavior));

        // Act
        List<GamificationBehaviorGroupDTO> result = groupService.getAllGroups();

        // Assert
        assertNotNull(result);
        assertNotNull(result.get(0).getBehaviors());
        assertEquals(1, result.get(0).getBehaviors().size());
        assertEquals(behavior.getCode(), result.get(0).getBehaviors().get(0).getCode());
    }

    @Test
    void getGroupById_WhenExists_ShouldReturnGroup() {
        // Arrange
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(behaviorRepository.findByGroupId(1)).thenReturn(List.of(behavior));

        // Act
        GamificationBehaviorGroupDTO result = groupService.getGroupById(1);

        // Assert
        assertNotNull(result);
        assertEquals(group.getName(), result.getName());
        verify(groupRepository).findById(1);
        verify(behaviorRepository).findByGroupId(1);
    }

    @Test
    void getGroupById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(groupRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            groupService.getGroupById(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy nhóm hành vi"));
        verify(groupRepository).findById(999);
    }

    @Test
    void createGroup_ShouldCreateAndReturnGroup() {
        // Arrange
        when(groupRepository.save(any(GamificationBehaviorGroup.class))).thenAnswer(invocation -> {
            GamificationBehaviorGroup saved = invocation.getArgument(0);
            saved.setId(1);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });
        when(behaviorRepository.findByGroupId(1)).thenReturn(new ArrayList<>());

        // Act
        GamificationBehaviorGroupDTO result = groupService.createGroup(groupDTO);

        // Assert
        assertNotNull(result);
        assertEquals(groupDTO.getName(), result.getName());
        verify(groupRepository).save(any(GamificationBehaviorGroup.class));
    }

    @Test
    void updateGroup_WhenExists_ShouldUpdateAndReturnGroup() {
        // Arrange
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(GamificationBehaviorGroup.class))).thenReturn(group);
        when(behaviorRepository.findByGroupId(1)).thenReturn(List.of(behavior));

        GamificationBehaviorGroupDTO updateDTO = new GamificationBehaviorGroupDTO();
        updateDTO.setName("Updated Name");

        // Act
        GamificationBehaviorGroupDTO result = groupService.updateGroup(1, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", group.getName());
        verify(groupRepository).findById(1);
        verify(groupRepository).save(group);
    }

    @Test
    void updateGroup_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(groupRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            groupService.updateGroup(999, groupDTO);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy nhóm hành vi"));
        verify(groupRepository).findById(999);
        verify(groupRepository, never()).save(any());
    }

    @Test
    void updateGroup_WithNullName_ShouldNotUpdate() {
        // Arrange
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(GamificationBehaviorGroup.class))).thenReturn(group);
        when(behaviorRepository.findByGroupId(1)).thenReturn(new ArrayList<>());

        GamificationBehaviorGroupDTO updateDTO = new GamificationBehaviorGroupDTO();
        updateDTO.setName(null);

        // Act
        groupService.updateGroup(1, updateDTO);

        // Assert
        // Original name should remain
        assertNotNull(group.getName());
        verify(groupRepository).save(group);
    }

    @Test
    void deleteGroup_WhenExists_ShouldDelete() {
        // Arrange
        when(groupRepository.existsById(1)).thenReturn(true);
        doNothing().when(groupRepository).deleteById(1);

        // Act
        groupService.deleteGroup(1);

        // Assert
        verify(groupRepository).existsById(1);
        verify(groupRepository).deleteById(1);
    }

    @Test
    void deleteGroup_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(groupRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            groupService.deleteGroup(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy nhóm hành vi"));
        verify(groupRepository).existsById(999);
        verify(groupRepository, never()).deleteById(anyInt());
    }

    @Test
    void mapToDTO_ShouldMapAllFields() {
        // Arrange
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(behaviorRepository.findByGroupId(1)).thenReturn(List.of(behavior));

        // Act
        GamificationBehaviorGroupDTO result = groupService.getGroupById(1);

        // Assert
        assertNotNull(result);
        assertEquals(group.getId(), result.getId());
        assertEquals(group.getName(), result.getName());
        assertEquals(group.getCreatedAt(), result.getCreatedAt());
        assertNotNull(result.getBehaviors());
    }

    @Test
    void mapToDTO_WhenGroupHasNoBehaviors_ShouldReturnEmptyBehaviorsList() {
        // Arrange
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(behaviorRepository.findByGroupId(1)).thenReturn(new ArrayList<>());

        // Act
        GamificationBehaviorGroupDTO result = groupService.getGroupById(1);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getBehaviors());
        assertTrue(result.getBehaviors().isEmpty());
    }

    @Test
    void mapBehaviorToDTO_ShouldMapAllFields() {
        // Arrange
        when(groupRepository.findById(1)).thenReturn(Optional.of(group));
        when(behaviorRepository.findByGroupId(1)).thenReturn(List.of(behavior));

        // Act
        GamificationBehaviorGroupDTO result = groupService.getGroupById(1);

        // Assert
        assertNotNull(result.getBehaviors());
        GamificationBehaviorDTO behaviorDTO = result.getBehaviors().get(0);
        assertEquals(behavior.getId(), behaviorDTO.getId());
        assertEquals(behavior.getCode(), behaviorDTO.getCode());
        assertEquals(behavior.getName(), behaviorDTO.getName());
        assertEquals(behavior.getFrequencyType().name(), behaviorDTO.getFrequencyType());
        assertEquals(behavior.getPointDiligence(), behaviorDTO.getPointDiligence());
        assertEquals(behavior.getPointCompetence(), behaviorDTO.getPointCompetence());
        assertEquals(behavior.getPointExperience(), behaviorDTO.getPointExperience());
    }
}

