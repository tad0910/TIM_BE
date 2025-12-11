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
    private GamificationPointTypeRepository pointTypeRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RankingService rankingService;
    @Mock
    private NotificationTemplateService notificationTemplateService;
    @Mock
    private UserService userService;

    @InjectMocks
    private GamificationService gamificationService;

    private GamificationBehavior behavior;
    private UserGamificationStats stats;
    private User user;

    @BeforeEach
    void setUp() {
        behavior = new GamificationBehavior();
        behavior.setId(1);
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

        user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setUsername("testuser");

        lenient().when(userService.findById(1L)).thenReturn(user);
        lenient().when(notificationTemplateService.render(any(), any())).thenReturn(null);
    }

    @Test
    void awardPoints_Success_UnlimitedFrequency() {
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        assertThat(response.getPointsDiligenceEarned()).isEqualTo(10);
        assertThat(response.getTotalDiligence()).isEqualTo(10);

        verify(pointLogRepository).save(any(UserPointLog.class));
        verify(statsRepository).save(any(UserGamificationStats.class));
        verify(rankingService).updateRanking(1L);
        verify(notificationService).createNotification(anyLong(), any(), any(), anyString(), anyLong(), anyString(),
                anyString(), any());
    }

    @Test
    void awardPoints_BehaviorNotFound_ShouldThrowException() {
        when(behaviorRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, 999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy hành vi");
    }

    @Test
    void awardPoints_FrequencyLimitExceeded_ShouldThrowException() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.DAILY);
        behavior.setMaxTimesPerFrequency(1);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(pointLogRepository.countByUserIdAndBehaviorIdAndDateRange(anyLong(), anyInt(), any(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đạt giới hạn điểm thưởng");
    }

    @Test
    void awardPoints_OnceFrequency_AlreadyAwarded_ShouldThrowException() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.ONCE);
        UserPointLog existingLog = new UserPointLog();
        existingLog.setBehavior(behavior);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(pointLogRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(existingLog));

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, 1))
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
        achievement.setId(1);
        achievement.setName("Achievement 1");
        level.setAchievement(achievement);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).hasSize(1);
        assertThat(response.getNewlyUnlockedAchievements().get(0).getLevelName()).isEqualTo("Level 1");

        verify(achievementRepository).save(any(UserAchievement.class));
        // Verify 2 notifications: 1 for points, 1 for achievement
        verify(notificationService, times(2)).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void awardPoints_NewPointTypeStructure_ShouldMapAndNotifyPerType() {
        behavior.setPointDiligence(0);
        behavior.setPointCompetence(0);
        behavior.setPointExperience(0);

        NotificationTemplate template1 = new NotificationTemplate();
        template1.setId(10L);
        NotificationTemplate template2 = new NotificationTemplate();
        template2.setId(11L);
        NotificationTemplate template3 = new NotificationTemplate();
        template3.setId(12L);

        BehaviorPointType diligence = new BehaviorPointType();
        diligence.setBehavior(behavior);
        GamificationPointType diligenceType = new GamificationPointType();
        diligenceType.setId(1);
        diligenceType.setName("Diligence");
        diligence.setPointType(diligenceType);
        diligence.setPoints(7);
        diligence.setNotificationTemplate(template1);

        BehaviorPointType competence = new BehaviorPointType();
        competence.setBehavior(behavior);
        GamificationPointType competenceType = new GamificationPointType();
        competenceType.setId(2);
        competenceType.setName("Năng lực");
        competence.setPointType(competenceType);
        competence.setPoints(3);
        competence.setNotificationTemplate(template2);

        BehaviorPointType custom = new BehaviorPointType();
        custom.setBehavior(behavior);
        GamificationPointType customType = new GamificationPointType();
        customType.setId(3);
        customType.setName("Creativity");
        custom.setPointType(customType);
        custom.setPoints(2);
        custom.setNotificationTemplate(template3);

        behavior.setBehaviorPointTypes(List.of(diligence, competence, custom));

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(notificationTemplateService.renderById(anyLong(), any())).thenReturn(
                new NotificationTemplateService.RenderedTemplate("t", "c", null)
        );

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getPointsDiligenceEarned()).isEqualTo(7);
        assertThat(response.getPointsCompetenceEarned()).isEqualTo(3);
        assertThat(response.getPointsExperienceEarned()).isEqualTo(2);
        assertThat(response.getTotalDiligence()).isEqualTo(7);
        assertThat(response.getTotalCompetence()).isEqualTo(3);
        assertThat(response.getTotalExperience()).isEqualTo(2);

        ArgumentCaptor<UserPointLog> logCaptor = ArgumentCaptor.forClass(UserPointLog.class);
        verify(pointLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getPointsDiligenceEarned()).isEqualTo(7);
        assertThat(logCaptor.getValue().getPointsCompetenceEarned()).isEqualTo(3);
        assertThat(logCaptor.getValue().getPointsExperienceEarned()).isEqualTo(2);

        verify(notificationService, times(3)).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
        verify(rankingService).updateRanking(1L);
    }

    @Test
    void awardPoints_RequiredPointTypeIdNotFound_ShouldSkipAchievement() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setMinPointsRequired(1);
        level.setRequiredPointTypeId(99);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(pointTypeRepository.findById(99)).thenReturn(Optional.empty());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).isEmpty();
        verify(achievementRepository, never()).save(any());
        verify(notificationService, times(1)).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void awardPoints_RequiredPointTypeIdUnlocks_ShouldNotify() {
        stats.setTotalCompetence(4);
        GamificationPointType competenceType = new GamificationPointType();
        competenceType.setId(5);
        competenceType.setName("Competence");

        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Pro");
        level.setMinPointsRequired(5);
        level.setRequiredPointTypeId(5);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Ach");
        level.setAchievement(achievement);

        behavior.setPointCompetence(2);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(Collections.emptyList());
        when(pointTypeRepository.findById(5)).thenReturn(Optional.of(competenceType));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).hasSize(1);
        assertThat(response.getNewlyUnlockedAchievements().get(0).getLevelName()).isEqualTo("Pro");
        verify(achievementRepository).save(any(UserAchievement.class));
        verify(notificationService, times(2)).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void awardPoints_UserNameLookupFails_ShouldStillSendNotification() {
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(userService.findById(anyLong())).thenThrow(new RuntimeException("fail"));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        verify(notificationService).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
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
        assertThat(logs.get(0).getBehaviorId()).isEqualTo(1);
        assertThat(logs.get(0).getBehaviorName()).isEqualTo("Test Behavior");
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
