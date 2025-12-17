package com.tim.appTim.service;

import com.tim.appTim.dto.AchievementWithStatusDTO;
import com.tim.appTim.dto.AwardPointsResponse;
import com.tim.appTim.dto.NotificationDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
    private NotificationRepository notificationRepository;
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

    @Test
    void awardPoints_WeeklyFrequency_WithinLimit_ShouldSucceed() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.WEEKLY);
        behavior.setMaxTimesPerFrequency(3);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(pointLogRepository.countByUserIdAndBehaviorIdAndDateRange(anyLong(), anyInt(), any(), any()))
                .thenReturn(2L);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        verify(pointLogRepository).save(any(UserPointLog.class));
    }

    @Test
    void awardPoints_WeeklyFrequency_ExceededLimit_ShouldThrowException() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.WEEKLY);
        behavior.setMaxTimesPerFrequency(2);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(pointLogRepository.countByUserIdAndBehaviorIdAndDateRange(anyLong(), anyInt(), any(), any()))
                .thenReturn(2L);

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void awardPoints_MonthlyFrequency_WithinLimit_ShouldSucceed() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.MONTHLY);
        behavior.setMaxTimesPerFrequency(5);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(pointLogRepository.countByUserIdAndBehaviorIdAndDateRange(anyLong(), anyInt(), any(), any()))
                .thenReturn(3L);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        verify(pointLogRepository).save(any(UserPointLog.class));
    }

    @Test
    void awardPoints_MonthlyFrequency_ExceededLimit_ShouldThrowException() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.MONTHLY);
        behavior.setMaxTimesPerFrequency(2);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(pointLogRepository.countByUserIdAndBehaviorIdAndDateRange(anyLong(), anyInt(), any(), any()))
                .thenReturn(2L);

        assertThatThrownBy(() -> gamificationService.awardPoints(1L, 1))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void awardPoints_OnceFrequency_NotAwarded_ShouldSucceed() {
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.ONCE);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(pointLogRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        verify(pointLogRepository).save(any(UserPointLog.class));
    }

    @Test
    void awardPoints_RankingServiceFails_ShouldStillAwardPoints() {
        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Ranking service error")).when(rankingService).updateRanking(1L);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        verify(pointLogRepository).save(any(UserPointLog.class));
    }

    @Test
    void awardPoints_AchievementAlreadyExists_ShouldNotCreateDuplicate() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Level 1");
        level.setMinPointsRequired(5);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Achievement 1");
        level.setAchievement(achievement);

        stats.setTotalDiligence(10);

        UserAchievement existingAchievement = new UserAchievement();
        existingAchievement.setId(1L);
        existingAchievement.setUserId(1L);
        existingAchievement.setAchievementLevel(level);
        existingAchievement.setIsDisplayed(true);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(List.of(existingAchievement));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).isEmpty();
        verify(achievementRepository, never()).save(any(UserAchievement.class));
    }

    @Test
    void awardPoints_AchievementExistsButNotDisplayed_ShouldSetDisplayed() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Level 1");
        level.setMinPointsRequired(5);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Achievement 1");
        level.setAchievement(achievement);

        stats.setTotalDiligence(10);

        UserAchievement existingAchievement = new UserAchievement();
        existingAchievement.setId(1L);
        existingAchievement.setUserId(1L);
        existingAchievement.setAchievementLevel(level);
        existingAchievement.setIsDisplayed(false);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(List.of(existingAchievement));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).isEmpty();
        verify(achievementRepository).saveAll(anyList());
        assertThat(existingAchievement.getIsDisplayed()).isTrue();
    }

    @Test
    void awardPoints_AchievementRevoked_ShouldHideAndNotify() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Level 1");
        level.setMinPointsRequired(20);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Achievement 1");
        level.setAchievement(achievement);

        stats.setTotalDiligence(5);

        UserAchievement existingAchievement = new UserAchievement();
        existingAchievement.setId(1L);
        existingAchievement.setUserId(1L);
        existingAchievement.setAchievementLevel(level);
        existingAchievement.setIsDisplayed(true);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(List.of(existingAchievement));
        when(notificationRepository.findByReceiverIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), any(), anyString(), anyLong())).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).isEmpty();
        verify(achievementRepository).saveAll(anyList());
        assertThat(existingAchievement.getIsDisplayed()).isFalse();
        verify(notificationService, atLeastOnce()).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void awardPoints_MultipleLevels_SameAchievement_ShouldNotifyHighestOnly() {
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Achievement 1");

        GamificationAchievementLevel level1 = new GamificationAchievementLevel();
        level1.setId(1);
        level1.setLevelName("Bronze");
        level1.setMinPointsRequired(10);
        level1.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level1.setAchievement(achievement);

        GamificationAchievementLevel level2 = new GamificationAchievementLevel();
        level2.setId(2);
        level2.setLevelName("Silver");
        level2.setMinPointsRequired(20);
        level2.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level2.setAchievement(achievement);

        stats.setTotalDiligence(20);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level1, level2));
        when(achievementRepository.findByUserIdAndAchievementLevelId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).hasSize(2);
        verify(achievementRepository, times(2)).save(any(UserAchievement.class));
    }

    @Test
    void awardPoints_CompetencePointType_ShouldMapCorrectly() {
        GamificationPointType competenceType = new GamificationPointType();
        competenceType.setId(1);
        competenceType.setName("Năng lực");

        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setMinPointsRequired(5);
        level.setRequiredPointTypeId(1);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Competence Achievement");
        level.setAchievement(achievement);

        stats.setTotalCompetence(6);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(Collections.emptyList());
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(competenceType));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).hasSize(1);
    }

    @Test
    void awardPoints_ExperiencePointType_ShouldMapCorrectly() {
        GamificationPointType experienceType = new GamificationPointType();
        experienceType.setId(1);
        experienceType.setName("Kinh nghiệm");

        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setMinPointsRequired(5);
        level.setRequiredPointTypeId(1);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Experience Achievement");
        level.setAchievement(achievement);

        stats.setTotalExperience(6);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(Collections.emptyList());
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(experienceType));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).hasSize(1);
    }

    @Test
    void awardPoints_NoPointTypeEnumOrId_ShouldSkipAchievement() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setMinPointsRequired(10);
        level.setRequiredPointTypeEnum(null);
        level.setRequiredPointTypeId(null);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getNewlyUnlockedAchievements()).isEmpty();
        verify(achievementRepository, never()).save(any());
    }

    @Test
    void awardPoints_NotificationTemplateRendered_ShouldUseRenderedContent() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        behavior.setNotificationTemplateDiligence(template);

        NotificationTemplateService.RenderedTemplate rendered = new NotificationTemplateService.RenderedTemplate(
                "Custom Title", "Custom Content", "icon.png");

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(notificationTemplateService.renderById(eq(1L), any())).thenReturn(rendered);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                titleCaptor.capture(), contentCaptor.capture(), any());
        assertThat(titleCaptor.getValue()).isEqualTo("Custom Title");
        assertThat(contentCaptor.getValue()).isEqualTo("Custom Content");
    }

    @Test
    void awardPoints_NoNotificationTemplates_ShouldUseFallback() {
        behavior.setNotificationTemplateDiligence(null);
        behavior.setNotificationTemplateCompetence(null);
        behavior.setNotificationTemplateExperience(null);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(notificationTemplateService.render(anyString(), any())).thenReturn(null);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        verify(notificationService).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void getUserPointLogs_WithPageable_ShouldReturnPage() {
        UserPointLog log = new UserPointLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setBehavior(behavior);

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserPointLog> logPage = new PageImpl<>(List.of(log));

        when(pointLogRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(logPage);

        Page<UserPointLogDTO> result = gamificationService.getUserPointLogs(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBehaviorId()).isEqualTo(1);
    }

    @Test
    void getUserAchievements_WithPageable_ShouldReturnPage() {
        UserAchievement ua = new UserAchievement();
        ua.setId(1L);
        ua.setUserId(1L);
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Level 1");
        ua.setAchievementLevel(level);

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserAchievement> achievementPage = new PageImpl<>(List.of(ua));

        when(achievementRepository.findByUserIdOrderByUnlockedAtDesc(1L, pageable)).thenReturn(achievementPage);

        Page<UserAchievementDTO> result = gamificationService.getUserAchievements(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLevelName()).isEqualTo("Level 1");
    }

    @Test
    void getUserPointLogs_WithNullBehavior_ShouldHandleGracefully() {
        UserPointLog log = new UserPointLog();
        log.setId(1L);
        log.setUserId(1L);
        log.setBehavior(null);

        when(pointLogRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(log));

        List<UserPointLogDTO> logs = gamificationService.getUserPointLogs(1L);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getBehaviorId()).isNull();
    }

    @Test
    void getUserAchievements_WithNullAchievementLevel_ShouldHandleGracefully() {
        UserAchievement ua = new UserAchievement();
        ua.setId(1L);
        ua.setUserId(1L);
        ua.setAchievementLevel(null);

        when(achievementRepository.findByUserIdOrderByUnlockedAtDesc(1L)).thenReturn(List.of(ua));

        List<UserAchievementDTO> achievements = gamificationService.getUserAchievements(1L);

        assertThat(achievements).hasSize(1);
        assertThat(achievements.get(0).getLevelName()).isNull();
    }

    @Test
    void checkAchievementsForAllUsers_ShouldProcessAllUsers() {
        UserGamificationStats stats1 = new UserGamificationStats();
        stats1.setUserId(1L);
        stats1.setTotalDiligence(10);
        stats1.setTotalCompetence(5);
        stats1.setTotalExperience(15);

        UserGamificationStats stats2 = new UserGamificationStats();
        stats2.setUserId(2L);
        stats2.setTotalDiligence(20);
        stats2.setTotalCompetence(10);
        stats2.setTotalExperience(30);

        when(statsRepository.findAll()).thenReturn(List.of(stats1, stats2));
        lenient().when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(achievementRepository.findByUserIdAndAchievementLevelId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        gamificationService.checkAchievementsForAllUsers();

        verify(statsRepository).findAll();
        verify(achievementLevelRepository, times(2)).findAll();
    }

    @Test
    void checkAchievementsForAllUsers_WithError_ShouldContinueProcessing() {
        UserGamificationStats stats1 = new UserGamificationStats();
        stats1.setUserId(1L);
        stats1.setTotalDiligence(10);

        UserGamificationStats stats2 = new UserGamificationStats();
        stats2.setUserId(2L);
        stats2.setTotalDiligence(20);

        when(statsRepository.findAll()).thenReturn(List.of(stats1, stats2));
        when(achievementLevelRepository.findAll())
                .thenReturn(Collections.emptyList())
                .thenThrow(new RuntimeException("Error"));

        gamificationService.checkAchievementsForAllUsers();

        verify(statsRepository).findAll();
    }

    @Test
    void getAllAchievementsWithUserStatus_ShouldReturnAchievementsWithStatus() {
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Test Achievement");
        achievement.setImageUrl("achievement.png");

        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Bronze");
        level.setMinPointsRequired(10);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level.setAchievement(achievement);

        stats.setTotalDiligence(15);

        UserAchievement savedAchievement = new UserAchievement();
        savedAchievement.setId(1L);
        savedAchievement.setUserId(1L);
        savedAchievement.setAchievementLevel(level);
        savedAchievement.setIsDisplayed(true);
        savedAchievement.setUnlockedAt(LocalDateTime.now());

        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(achievementRepository.save(any(UserAchievement.class))).thenAnswer(i -> {
            UserAchievement ua = i.getArgument(0);
            ua.setId(1L);
            ua.setUnlockedAt(LocalDateTime.now());
            return ua;
        });
        // findDisplayedByUserId được gọi sau khi checkAndUnlockAchievements, nên cần trả về saved achievement
        when(achievementRepository.findDisplayedByUserId(1L))
                .thenReturn(List.of(savedAchievement));
        when(statsRepository.count()).thenReturn(100L);
        when(achievementRepository.countDistinctUsersByAchievementId(1)).thenReturn(10L);

        List<AchievementWithStatusDTO> result = gamificationService.getAllAchievementsWithUserStatus(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAchievementId()).isEqualTo(1);
        assertThat(result.get(0).getCurrentPoints()).isEqualTo(15);
        assertThat(result.get(0).getIsUnlocked()).isTrue();
    }

    @Test
    void getAllAchievementsWithUserStatus_WithMultipleLevels_ShouldReturnHighestUnlocked() {
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Test Achievement");

        GamificationAchievementLevel level1 = new GamificationAchievementLevel();
        level1.setId(1);
        level1.setLevelName("Bronze");
        level1.setMinPointsRequired(10);
        level1.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level1.setAchievement(achievement);

        GamificationAchievementLevel level2 = new GamificationAchievementLevel();
        level2.setId(2);
        level2.setLevelName("Silver");
        level2.setMinPointsRequired(20);
        level2.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level2.setAchievement(achievement);

        stats.setTotalDiligence(25);

        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setId(1L);
        userAchievement.setUserId(1L);
        userAchievement.setAchievementLevel(level2);
        userAchievement.setIsDisplayed(true);
        userAchievement.setUnlockedAt(LocalDateTime.now());

        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level1, level2));
        when(achievementRepository.findDisplayedByUserId(1L)).thenReturn(List.of(userAchievement));
        when(achievementRepository.findByUserIdAndAchievementLevelId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(statsRepository.count()).thenReturn(100L);
        when(achievementRepository.countDistinctUsersByAchievementId(1)).thenReturn(10L);

        List<AchievementWithStatusDTO> result = gamificationService.getAllAchievementsWithUserStatus(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHighestUnlockedLevelName()).isEqualTo("Silver");
        assertThat(result.get(0).getIsUnlocked()).isTrue();
    }

    @Test
    void getAllAchievementsWithUserStatus_WithRequiredPointTypeId_ShouldMapCorrectly() {
        GamificationPointType pointType = new GamificationPointType();
        pointType.setId(1);
        pointType.setName("Chuyên cần");

        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Test Achievement");

        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Bronze");
        level.setMinPointsRequired(10);
        level.setRequiredPointTypeId(1);
        level.setAchievement(achievement);

        stats.setTotalDiligence(15);

        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findDisplayedByUserId(1L)).thenReturn(Collections.emptyList());
        when(achievementRepository.findByUserIdAndAchievementLevelId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(pointTypeRepository.findById(1)).thenReturn(Optional.of(pointType));
        when(statsRepository.count()).thenReturn(100L);
        when(achievementRepository.countDistinctUsersByAchievementId(1)).thenReturn(10L);

        List<AchievementWithStatusDTO> result = gamificationService.getAllAchievementsWithUserStatus(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentPoints()).isEqualTo(15);
    }

    @Test
    void getAllAchievementsWithUserStatus_NewUser_ShouldCreateStats() {
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Test Achievement");

        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Bronze");
        level.setMinPointsRequired(10);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level.setAchievement(achievement);

        when(statsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(statsRepository.save(any(UserGamificationStats.class))).thenAnswer(i -> {
            UserGamificationStats stats = i.getArgument(0);
            stats.setUserId(1L);
            return stats;
        });
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findDisplayedByUserId(1L)).thenReturn(Collections.emptyList());
        when(achievementRepository.findByUserIdAndAchievementLevelId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(statsRepository.count()).thenReturn(100L);
        when(achievementRepository.countDistinctUsersByAchievementId(1)).thenReturn(10L);

        List<AchievementWithStatusDTO> result = gamificationService.getAllAchievementsWithUserStatus(1L);

        assertThat(result).hasSize(1);
        verify(statsRepository).save(any(UserGamificationStats.class));
    }

    @Test
    void getAllAchievementsWithUserStatus_WithError_ShouldContinueProcessing() {
        GamificationAchievement achievement1 = new GamificationAchievement();
        achievement1.setId(1);
        achievement1.setName("Achievement 1");

        GamificationAchievement achievement2 = new GamificationAchievement();
        achievement2.setId(2);
        achievement2.setName("Achievement 2");

        GamificationAchievementLevel level1 = new GamificationAchievementLevel();
        level1.setId(1);
        level1.setLevelName("Level 1");
        level1.setMinPointsRequired(10);
        level1.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level1.setAchievement(achievement1);

        GamificationAchievementLevel level2 = new GamificationAchievementLevel();
        level2.setId(2);
        level2.setLevelName("Level 2");
        level2.setMinPointsRequired(10);
        level2.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        level2.setAchievement(achievement2);

        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level1, level2));
        when(achievementRepository.findDisplayedByUserId(1L)).thenReturn(Collections.emptyList());
        when(achievementRepository.findByUserIdAndAchievementLevelId(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(statsRepository.count()).thenReturn(100L);
        when(achievementRepository.countDistinctUsersByAchievementId(1)).thenReturn(10L);
        when(achievementRepository.countDistinctUsersByAchievementId(2))
                .thenThrow(new RuntimeException("Error"));

        List<AchievementWithStatusDTO> result = gamificationService.getAllAchievementsWithUserStatus(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void sendAchievementUnlockedNotification_WithTemplate_ShouldUseTemplate() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Gold");
        level.setMinPointsRequired(5);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Test Achievement");
        level.setAchievement(achievement);

        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        level.setNotificationTemplate(template);

        NotificationTemplateService.RenderedTemplate rendered = new NotificationTemplateService.RenderedTemplate(
                "Custom Title", "Custom Content", "icon.png");

        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setId(1L);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(Collections.emptyList());
        when(notificationTemplateService.renderById(eq(1L), any())).thenReturn(rendered);
        when(notificationService.createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any())).thenReturn(notificationDTO);

        gamificationService.awardPoints(1L, 1);

        verify(notificationTemplateService).renderById(eq(1L), any());
    }

    @Test
    void sendAchievementUnlockedNotification_NotificationCreationFails_ShouldNotThrow() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Gold");
        level.setMinPointsRequired(5);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Test Achievement");
        level.setAchievement(achievement);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(Collections.emptyList());
        when(notificationService.createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any())).thenReturn(null);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
    }

    @Test
    void sendAchievementRevokedNotification_DuplicateCheck_ShouldSkipIfRecent() {
        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setId(1);
        level.setLevelName("Gold");
        level.setMinPointsRequired(20);
        level.setRequiredPointTypeEnum(GamificationAchievementLevel.PointTypeEnum.DILIGENCE);
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setId(1);
        achievement.setName("Test Achievement");
        level.setAchievement(achievement);

        stats.setTotalDiligence(5);

        UserAchievement existingAchievement = new UserAchievement();
        existingAchievement.setId(1L);
        existingAchievement.setUserId(1L);
        existingAchievement.setAchievementLevel(level);
        existingAchievement.setIsDisplayed(true);

        Notification recentNotification = new Notification();
        recentNotification.setTitle("Bạn không còn đủ điều kiện cho huy hiệu");
        recentNotification.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(List.of(level));
        when(achievementRepository.findByUserIdAndAchievementLevelId(1L, 1)).thenReturn(List.of(existingAchievement));
        when(notificationRepository.findByReceiverIdAndNotificationTypeAndTargetTypeAndTargetId(
                anyLong(), any(), anyString(), anyLong())).thenReturn(List.of(recentNotification));

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
        verify(notificationService, times(1)).createNotification(anyLong(), any(), any(), anyString(), anyLong(),
                anyString(), anyString(), any());
    }

    @Test
    void getUserFullNameSafe_WithFirstNameAndLastName_ShouldReturnFullName() {
        user.setFirstName("John");
        user.setLastName("Doe");

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(userService.findById(1L)).thenReturn(user);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
    }

    @Test
    void getUserFullNameSafe_WithOnlyUsername_ShouldReturnUsername() {
        user.setFirstName(null);
        user.setLastName(null);

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());
        when(userService.findById(1L)).thenReturn(user);

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response).isNotNull();
    }

    @Test
    void awardPoints_BehaviorPointTypesWithNullPoints_ShouldSkip() {
        behavior.setPointDiligence(0);
        behavior.setPointCompetence(0);
        behavior.setPointExperience(0);

        BehaviorPointType validBpt = new BehaviorPointType();
        validBpt.setBehavior(behavior);
        GamificationPointType pointType = new GamificationPointType();
        pointType.setName("Diligence");
        validBpt.setPointType(pointType);
        validBpt.setPoints(5);

        BehaviorPointType nullPointsBpt = new BehaviorPointType();
        nullPointsBpt.setBehavior(behavior);
        nullPointsBpt.setPointType(pointType);
        nullPointsBpt.setPoints(null);

        behavior.setBehaviorPointTypes(List.of(validBpt, nullPointsBpt));

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getPointsDiligenceEarned()).isEqualTo(5);
    }

    @Test
    void awardPoints_BehaviorPointTypesWithZeroPoints_ShouldSkip() {
        behavior.setPointDiligence(0);
        behavior.setPointCompetence(0);
        behavior.setPointExperience(0);

        BehaviorPointType validBpt = new BehaviorPointType();
        validBpt.setBehavior(behavior);
        GamificationPointType pointType = new GamificationPointType();
        pointType.setName("Diligence");
        validBpt.setPointType(pointType);
        validBpt.setPoints(5);

        BehaviorPointType zeroPointsBpt = new BehaviorPointType();
        zeroPointsBpt.setBehavior(behavior);
        zeroPointsBpt.setPointType(pointType);
        zeroPointsBpt.setPoints(0);

        behavior.setBehaviorPointTypes(List.of(validBpt, zeroPointsBpt));

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getPointsDiligenceEarned()).isEqualTo(5);
    }

    @Test
    void awardPoints_BehaviorPointTypesWithNullPointType_ShouldSkip() {
        behavior.setPointDiligence(0);
        behavior.setPointCompetence(0);
        behavior.setPointExperience(0);

        BehaviorPointType validBpt = new BehaviorPointType();
        validBpt.setBehavior(behavior);
        GamificationPointType pointType = new GamificationPointType();
        pointType.setName("Diligence");
        validBpt.setPointType(pointType);
        validBpt.setPoints(5);

        BehaviorPointType nullPointTypeBpt = new BehaviorPointType();
        nullPointTypeBpt.setBehavior(behavior);
        nullPointTypeBpt.setPointType(null);
        nullPointTypeBpt.setPoints(3);

        behavior.setBehaviorPointTypes(List.of(validBpt, nullPointTypeBpt));

        when(behaviorRepository.findById(1)).thenReturn(Optional.of(behavior));
        when(statsRepository.findByUserId(1L)).thenReturn(Optional.of(stats));
        when(achievementLevelRepository.findAll()).thenReturn(Collections.emptyList());

        AwardPointsResponse response = gamificationService.awardPoints(1L, 1);

        assertThat(response.getPointsDiligenceEarned()).isEqualTo(5);
    }
}
