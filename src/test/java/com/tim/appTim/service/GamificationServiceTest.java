package com.tim.appTim.service;

import com.tim.appTim.dto.AwardPointsResponse;
import com.tim.appTim.dto.UserAchievementDTO;
import com.tim.appTim.dto.UserGamificationStatsDTO;
import com.tim.appTim.dto.UserPointLogDTO;
import com.tim.appTim.entity.*;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private GamificationBehaviorRepository behaviorRepository;
    @Mock
    private UserPointLogRepository pointLogRepository;
    @Mock
    private UserGamificationStatsRepository statsRepository;
    @Mock
    private UserAchievementRepository achievementRepository;
    @Mock
    private GamificationAchievementLevelRepository achievementLevelRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RankingService rankingService;

    @InjectMocks
    private GamificationService gamificationService;

    private GamificationBehavior behavior;
    private UserGamificationStats stats;

    @BeforeEach
    void setUp() {
        behavior = new GamificationBehavior();
        behavior.setId(1);
        behavior.setCode("TEST_BEHAVIOR");
        behavior.setName("Test Behavior");
        behavior.setPointDiligence(10);
        behavior.setPointCompetence(5);
        behavior.setPointExperience(15);
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.UNLIMITED);

        stats = new UserGamificationStats();
        stats.setUserId(1L);
        stats.setTotalDiligence(0);
        stats.setTotalCompetence(0);
        stats.setTotalExperience(0);
    }

    @Test
    void awardPoints_Success_UnlimitedFrequency() {
        when(behaviorRepository.findByCode("TEST_BEHAVIOR")).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, "TEST_BEHAVIOR");

        assertThat(response).isNotNull();
        assertThat(response.getPointsDiligenceEarned()).isEqualTo(10);
        assertThat(response.getTotalDiligence()).isEqualTo(10);

        verify(pointLogRepository).save(any(UserPointLog.class));
        verify(statsRepository).save(any(UserGamificationStats.class));
        verify(rankingService).updateRanking(1L);
        verify(notificationService).createNotification(anyLong(), any(), any(), anyString(), anyLong(), anyString(),
                anyString());
    }

    @Test
    void awardPoints_BehaviorNotFound_ShouldThrowException() {
        when(behaviorRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, "INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy hành vi");
    }

    @Test
    void awardPoints_FrequencyLimitExceeded_ShouldThrowException() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.DAILY);
        behavior.setMaxTimesPerFrequency(1);

        when(behaviorRepository.findByCode("TEST_BEHAVIOR")).thenReturn(Optional.of(behavior));
        when(pointLogRepository.countByUserIdAndBehaviorIdAndDateRange(anyLong(), anyInt(), any(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, "TEST_BEHAVIOR"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đạt giới hạn điểm thưởng");
    }

    @Test
    void awardPoints_OnceFrequency_AlreadyAwarded_ShouldThrowException() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.ONCE);
        UserPointLog existingLog = new UserPointLog();
        existingLog.setBehavior(behavior);

        when(behaviorRepository.findByCode("TEST_BEHAVIOR")).thenReturn(Optional.of(behavior));
        when(pointLogRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(existingLog));

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, "TEST_BEHAVIOR"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đạt giới hạn điểm thưởng");
    }

    @Test
    void awardPoints_ShouldUnlockAchievement() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Level 1");
        level.setMinPointsRequired(10);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setName("Achievement 1");
        level.setAchievement(achievement);

        when(behaviorRepository.findByCode("TEST_BEHAVIOR")).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, "TEST_BEHAVIOR");

        assertThat(response.getNewlyUnlockedAchievements()).hasSize(1);
        assertThat(response.getNewlyUnlockedAchievements().get(0).getLevelName()).isEqualTo("Level 1");

        verify(achievementRepository).save(any(UserAchievement.class));
        // Verify 2 notifications: 1 for points, 1 for achievement
        verify(notificationService, times(2)).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString());
    }

    @Test
    void getUserStats_Success() {
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));

        UserGamificationStatsDTO dto = gamificationService.getUserStats(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.getUserId()).isEqualTo(1L);
    }

    @Test
    void getUserStats_NewUser_ShouldCreateStats() {
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(statsRepository.save(any(UserGamificationStats.class))).thenAnswer(i -> i.getArgument(0));

        UserGamificationStatsDTO dto = gamificationService.getUserStats(1L);

        assertThat(dto).isNotNull();
        verify(statsRepository).save(any(UserGamificationStats.class));
    }

    @Test
    void getUserPointLogs_ShouldReturnList() {
        UserPointLog log = new UserPointLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setBehavior(behavior);

        when(pointLogRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(log));

        List<UserPointLogDTO> logs = gamificationService.getUserPointLogs(1L);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getBehaviorCode()).isEqualTo("TEST_BEHAVIOR");
    }

    @Test
    void getUserAchievements_ShouldReturnList() {
        UserAchievement ua = new UserAchievement();
        ua.setId(1L);
        ua.setUserId(1L);
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Level 1");
        ua.setAchievementLevel(level);

        when(achievementRepository.findByUserIdOrderByUnlockedAtDesc(1L)).thenReturn(List.of(ua));

        List<UserAchievementDTO> achievements = gamificationService.getUserAchievements(1L);

        assertThat(achievements).hasSize(1);
        assertThat(achievements.get(0).getLevelName()).isEqualTo("Level 1");
    }
}
