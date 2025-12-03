package com.tim.appTim.service;

import com.tim.appTim.dto.AchievementLevelDTO;
import com.tim.appTim.entity.GamificationAchievement;
import com.tim.appTim.entity.GamificationAchievementLevel;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.GamificationAchievementRepository;
import com.tim.appTim.repository.GamificationAchievementLevelRepository;
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
class GamificationAchievementServiceTest {

    @Mock
    private GamificationAchievementRepository achievementRepository;

    @Mock
    private GamificationAchievementLevelRepository levelRepository;

    @InjectMocks
    private GamificationAchievementService achievementService;

    private GamificationAchievement achievement;
    private GamificationAchievementLevel level;
    private AchievementLevelDTO levelDTO;

    @BeforeEach
    void setUp() {
        achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Học tập xuất sắc");
        achievement.setImageUrl("https://example.com/image.jpg");
        achievement.setCreatedBy(1);
        achievement.setCreatedAt(LocalDateTime.now());

        level = new GamificationAchievementLevel();
        level.setId(1);
        level.setAchievement(achievement);
        level.setLevelName("Cấp 1");
        level.setRequiredPointTypeId(1);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level.setMinPointsRequired(100);
        level.setImageUrl("https://example.com/level1.jpg");
        level.setCreatedAt(LocalDateTime.now());

        levelDTO = new AchievementLevelDTO();
        levelDTO.setAchievementId(1);
        levelDTO.setLevelName("Cấp 1");
        levelDTO.setRequiredPointTypeId(1);
        levelDTO.setRequiredPointTypeEnum("DILIGENCE");
        levelDTO.setMinPointsRequired(100);
        levelDTO.setImageUrl("https://example.com/level1.jpg");
    }

    @Test
    void getAllAchievements_ShouldReturnAllAchievements() {
        // Arrange
        List<GamificationAchievement> achievements = List.of(achievement);
        when(achievementRepository.findAll()).thenReturn(achievements);

        // Act
        List<GamificationAchievement> result = achievementService.getAllAchievements();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(achievement.getName(), result.get(0).getName());
        verify(achievementRepository).findAll();
    }

    @Test
    void getAllAchievements_WhenNoAchievements_ShouldReturnEmptyList() {
        // Arrange
        when(achievementRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<GamificationAchievement> result = achievementService.getAllAchievements();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(achievementRepository).findAll();
    }

    @Test
    void getAchievementById_WhenExists_ShouldReturnAchievement() {
        // Arrange
        when(achievementRepository.findById(1)).thenReturn(Optional.of(achievement));

        // Act
        GamificationAchievement result = achievementService.getAchievementById(1);

        // Assert
        assertNotNull(result);
        assertEquals(achievement.getName(), result.getName());
        verify(achievementRepository).findById(1);
    }

    @Test
    void getAchievementById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(achievementRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            achievementService.getAchievementById(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy thành tích"));
        verify(achievementRepository).findById(999);
    }

    @Test
    void getAchievementLevels_ShouldReturnLevelsOrderedByMinPoints() {
        // Arrange
        List<GamificationAchievementLevel> levels = List.of(level);
        when(levelRepository.findByAchievementIdOrderByMinPointsRequiredAsc(1)).thenReturn(levels);

        // Act
        List<AchievementLevelDTO> result = achievementService.getAchievementLevels(1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(level.getLevelName(), result.get(0).getLevelName());
        verify(levelRepository).findByAchievementIdOrderByMinPointsRequiredAsc(1);
    }

    @Test
    void getAchievementLevelById_WhenExists_ShouldReturnLevel() {
        // Arrange
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        // Act
        AchievementLevelDTO result = achievementService.getAchievementLevelById(1);

        // Assert
        assertNotNull(result);
        assertEquals(level.getLevelName(), result.getLevelName());
        assertEquals(level.getMinPointsRequired(), result.getMinPointsRequired());
        verify(levelRepository).findById(1);
    }

    @Test
    void getAchievementLevelById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(levelRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            achievementService.getAchievementLevelById(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy cấp bậc thành tích"));
        verify(levelRepository).findById(999);
    }

    @Test
    void createAchievement_ShouldCreateAndReturnAchievement() {
        // Arrange
        when(achievementRepository.save(any(GamificationAchievement.class))).thenAnswer(invocation -> {
            GamificationAchievement saved = invocation.getArgument(0);
            saved.setId(1);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        GamificationAchievement result = achievementService.createAchievement("Test Achievement", "https://image.jpg", 1);

        // Assert
        assertNotNull(result);
        assertEquals("Test Achievement", result.getName());
        assertEquals("https://image.jpg", result.getImageUrl());
        assertEquals(1, result.getCreatedBy());
        verify(achievementRepository).save(any(GamificationAchievement.class));
    }

    @Test
    void updateAchievement_WhenExists_ShouldUpdateAndReturnAchievement() {
        // Arrange
        when(achievementRepository.findById(1)).thenReturn(Optional.of(achievement));
        when(achievementRepository.save(any(GamificationAchievement.class))).thenReturn(achievement);

        // Act
        GamificationAchievement result = achievementService.updateAchievement(1, "Updated Name", "https://new-image.jpg");

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", achievement.getName());
        assertEquals("https://new-image.jpg", achievement.getImageUrl());
        verify(achievementRepository).findById(1);
        verify(achievementRepository).save(achievement);
    }

    @Test
    void updateAchievement_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(achievementRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            achievementService.updateAchievement(999, "Name", "url");
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy thành tích"));
        verify(achievementRepository).findById(999);
        verify(achievementRepository, never()).save(any());
    }

    @Test
    void updateAchievement_WithNullName_ShouldNotUpdateName() {
        // Arrange
        when(achievementRepository.findById(1)).thenReturn(Optional.of(achievement));
        when(achievementRepository.save(any(GamificationAchievement.class))).thenReturn(achievement);

        // Act
        achievementService.updateAchievement(1, null, "https://new-image.jpg");

        // Assert
        // Original name should remain
        assertNotNull(achievement.getName());
        assertEquals("https://new-image.jpg", achievement.getImageUrl());
        verify(achievementRepository).save(achievement);
    }

    @Test
    void updateAchievement_WithNullImageUrl_ShouldNotUpdateImageUrl() {
        // Arrange
        when(achievementRepository.findById(1)).thenReturn(Optional.of(achievement));
        when(achievementRepository.save(any(GamificationAchievement.class))).thenReturn(achievement);

        // Act
        achievementService.updateAchievement(1, "Updated Name", null);

        // Assert
        assertEquals("Updated Name", achievement.getName());
        // Original imageUrl should remain
        assertNotNull(achievement.getImageUrl());
        verify(achievementRepository).save(achievement);
    }

    @Test
    void deleteAchievement_WhenExists_ShouldDelete() {
        // Arrange
        when(achievementRepository.existsById(1)).thenReturn(true);
        doNothing().when(achievementRepository).deleteById(1);

        // Act
        achievementService.deleteAchievement(1);

        // Assert
        verify(achievementRepository).existsById(1);
        verify(achievementRepository).deleteById(1);
    }

    @Test
    void deleteAchievement_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(achievementRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            achievementService.deleteAchievement(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy thành tích"));
        verify(achievementRepository).existsById(999);
        verify(achievementRepository, never()).deleteById(anyInt());
    }

    @Test
    void createAchievementLevel_ShouldCreateAndReturnLevel() {
        // Arrange
        when(achievementRepository.findById(1)).thenReturn(Optional.of(achievement));
        when(levelRepository.save(any(GamificationAchievementLevel.class))).thenAnswer(invocation -> {
            GamificationAchievementLevel saved = invocation.getArgument(0);
            saved.setId(1);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        AchievementLevelDTO result = achievementService.createAchievementLevel(levelDTO);

        // Assert
        assertNotNull(result);
        assertEquals(levelDTO.getLevelName(), result.getLevelName());
        assertEquals(levelDTO.getMinPointsRequired(), result.getMinPointsRequired());
        verify(achievementRepository).findById(1);
        verify(levelRepository).save(any(GamificationAchievementLevel.class));
    }

    @Test
    void createAchievementLevel_WhenAchievementNotExists_ShouldThrowException() {
        // Arrange
        when(achievementRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            levelDTO.setAchievementId(999);
            achievementService.createAchievementLevel(levelDTO);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy thành tích"));
        verify(achievementRepository).findById(999);
        verify(levelRepository, never()).save(any());
    }

    @Test
    void createAchievementLevel_WithNullPointTypeEnum_ShouldHandleGracefully() {
        // Arrange
        levelDTO.setRequiredPointTypeEnum(null);
        when(achievementRepository.findById(1)).thenReturn(Optional.of(achievement));
        when(levelRepository.save(any(GamificationAchievementLevel.class))).thenAnswer(invocation -> {
            GamificationAchievementLevel saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        // Act
        AchievementLevelDTO result = achievementService.createAchievementLevel(levelDTO);

        // Assert
        assertNotNull(result);
        assertNull(result.getRequiredPointTypeEnum());
        verify(levelRepository).save(any(GamificationAchievementLevel.class));
    }

    @Test
    void updateAchievementLevel_WhenExists_ShouldUpdateAndReturnLevel() {
        // Arrange
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));
        when(levelRepository.save(any(GamificationAchievementLevel.class))).thenReturn(level);

        AchievementLevelDTO updateDTO = new AchievementLevelDTO();
        updateDTO.setLevelName("Updated Level");
        updateDTO.setMinPointsRequired(200);

        // Act
        AchievementLevelDTO result = achievementService.updateAchievementLevel(1, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Level", level.getLevelName());
        assertEquals(200, level.getMinPointsRequired());
        verify(levelRepository).findById(1);
        verify(levelRepository).save(level);
    }

    @Test
    void updateAchievementLevel_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(levelRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            achievementService.updateAchievementLevel(999, levelDTO);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy cấp bậc thành tích"));
        verify(levelRepository).findById(999);
        verify(levelRepository, never()).save(any());
    }

    @Test
    void updateAchievementLevel_WithNewAchievement_ShouldUpdateAchievement() {
        // Arrange
        GamificationAchievement newAchievement = new GamificationAchievement();
        newAchievement.setId(2);
        newAchievement.setName("New Achievement");

        when(levelRepository.findById(1)).thenReturn(Optional.of(level));
        when(achievementRepository.findById(2)).thenReturn(Optional.of(newAchievement));
        when(levelRepository.save(any(GamificationAchievementLevel.class))).thenReturn(level);

        AchievementLevelDTO updateDTO = new AchievementLevelDTO();
        updateDTO.setAchievementId(2);

        // Act
        achievementService.updateAchievementLevel(1, updateDTO);

        // Assert
        assertEquals(newAchievement, level.getAchievement());
        verify(achievementRepository).findById(2);
        verify(levelRepository).save(level);
    }

    @Test
    void updateAchievementLevel_WithNullFields_ShouldNotUpdate() {
        // Arrange
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));
        when(levelRepository.save(any(GamificationAchievementLevel.class))).thenReturn(level);

        AchievementLevelDTO updateDTO = new AchievementLevelDTO();
        // All fields are null

        // Act
        achievementService.updateAchievementLevel(1, updateDTO);

        // Assert
        // Original values should remain
        assertNotNull(level.getLevelName());
        verify(levelRepository).save(level);
    }

    @Test
    void deleteAchievementLevel_WhenExists_ShouldDelete() {
        // Arrange
        when(levelRepository.existsById(1)).thenReturn(true);
        doNothing().when(levelRepository).deleteById(1);

        // Act
        achievementService.deleteAchievementLevel(1);

        // Assert
        verify(levelRepository).existsById(1);
        verify(levelRepository).deleteById(1);
    }

    @Test
    void deleteAchievementLevel_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(levelRepository.existsById(999)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            achievementService.deleteAchievementLevel(999);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy cấp bậc thành tích"));
        verify(levelRepository).existsById(999);
        verify(levelRepository, never()).deleteById(anyInt());
    }

    @Test
    void mapToLevelDTO_ShouldMapAllFields() {
        // Arrange
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        // Act
        AchievementLevelDTO result = achievementService.getAchievementLevelById(1);

        // Assert
        assertNotNull(result);
        assertEquals(level.getId(), result.getId());
        assertEquals(level.getLevelName(), result.getLevelName());
        assertEquals(level.getRequiredPointTypeId(), result.getRequiredPointTypeId());
        assertEquals(level.getRequiredPointTypeEnum().name(), result.getRequiredPointTypeEnum());
        assertEquals(level.getMinPointsRequired(), result.getMinPointsRequired());
        assertEquals(level.getImageUrl(), result.getImageUrl());
        assertEquals(level.getCreatedAt(), result.getCreatedAt());
        assertEquals(achievement.getId(), result.getAchievementId());
        assertEquals(achievement.getName(), result.getAchievementName());
    }

    @Test
    void mapToLevelDTO_WhenAchievementIsNull_ShouldHandleGracefully() {
        // Arrange
        level.setAchievement(null);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        // Act
        AchievementLevelDTO result = achievementService.getAchievementLevelById(1);

        // Assert
        assertNotNull(result);
        assertNull(result.getAchievementId());
        assertNull(result.getAchievementName());
    }

    @Test
    void mapToLevelDTO_WhenPointTypeEnumIsNull_ShouldHandleGracefully() {
        // Arrange
        level.setRequiredPointTypeEnum(null);
        when(levelRepository.findById(1)).thenReturn(Optional.of(level));

        // Act
        AchievementLevelDTO result = achievementService.getAchievementLevelById(1);

        // Assert
        assertNotNull(result);
        assertNull(result.getRequiredPointTypeEnum());
    }
}

