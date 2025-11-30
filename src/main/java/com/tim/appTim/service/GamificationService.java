package com.tim.appTim.service;

import com.tim.appTim.dto.*;
import com.tim.appTim.entity.*;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationService {

    private final GamificationBehaviorRepository behaviorRepository;
    private final UserPointLogRepository pointLogRepository;
    private final UserGamificationStatsRepository statsRepository;
    private final UserAchievementRepository achievementRepository;
    private final GamificationAchievementLevelRepository achievementLevelRepository;
    private final NotificationService notificationService;
    private final RankingService rankingService;

    public GamificationService(
            GamificationBehaviorRepository behaviorRepository,
            UserPointLogRepository pointLogRepository,
            UserGamificationStatsRepository statsRepository,
            UserAchievementRepository achievementRepository,
            GamificationAchievementLevelRepository achievementLevelRepository,
            NotificationService notificationService,
            @Lazy RankingService rankingService) {
        this.behaviorRepository = behaviorRepository;
        this.pointLogRepository = pointLogRepository;
        this.statsRepository = statsRepository;
        this.achievementRepository = achievementRepository;
        this.achievementLevelRepository = achievementLevelRepository;
        this.notificationService = notificationService;
        this.rankingService = rankingService;
    }

    public AwardPointsResponse awardPoints(Long userId, String behaviorCode) {
        GamificationBehavior behavior = behaviorRepository.findByCode(behaviorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành vi với mã: " + behaviorCode));

        if (!canAwardPoints(userId, behavior)) {
            throw new BadRequestException("Bạn đã đạt giới hạn điểm thưởng cho hành vi này trong khoảng thời gian hiện tại");
        }

        UserPointLog pointLog = new UserPointLog();
        pointLog.setUserId(userId);
        pointLog.setBehavior(behavior);
        pointLog.setPointsDiligenceEarned(behavior.getPointDiligence());
        pointLog.setPointsCompetenceEarned(behavior.getPointCompetence());
        pointLog.setPointsExperienceEarned(behavior.getPointExperience());
        pointLogRepository.save(pointLog);

        UserGamificationStats stats = getOrCreateUserStats(userId);
        stats.setTotalDiligence(stats.getTotalDiligence() + behavior.getPointDiligence());
        stats.setTotalCompetence(stats.getTotalCompetence() + behavior.getPointCompetence());
        stats.setTotalExperience(stats.getTotalExperience() + behavior.getPointExperience());
        statsRepository.save(stats);

        try {
            rankingService.updateRanking(userId);
        } catch (Exception e) {

            System.err.println("Failed to update ranking for user " + userId + ": " + e.getMessage());
        }

        List<UserAchievementDTO> newlyUnlocked = checkAndUnlockAchievements(userId, stats);

        sendPointEarnedNotification(userId, behavior, 
                behavior.getPointDiligence(), 
                behavior.getPointCompetence(), 
                behavior.getPointExperience());

        AwardPointsResponse response = new AwardPointsResponse();
        response.setUserId(userId);
        response.setPointsDiligenceEarned(behavior.getPointDiligence());
        response.setPointsCompetenceEarned(behavior.getPointCompetence());
        response.setPointsExperienceEarned(behavior.getPointExperience());
        response.setTotalDiligence(stats.getTotalDiligence());
        response.setTotalCompetence(stats.getTotalCompetence());
        response.setTotalExperience(stats.getTotalExperience());
        response.setNewlyUnlockedAchievements(newlyUnlocked);
        response.setMessage("Bạn đã nhận được điểm thưởng!");

        return response;
    }

    private boolean canAwardPoints(Long userId, GamificationBehavior behavior) {
        GamificationBehavior.FrequencyType frequencyType = behavior.getFrequencyType();
        
        if (frequencyType == GamificationBehavior.FrequencyType.UNLIMITED) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = null;
        LocalDateTime endDate = now;

        switch (frequencyType) {
            case DAILY:
                startDate = now.truncatedTo(ChronoUnit.DAYS);
                break;
            case WEEKLY:
                startDate = now.minusDays(now.getDayOfWeek().getValue() - 1).truncatedTo(ChronoUnit.DAYS);
                break;
            case MONTHLY:
                startDate = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                break;
            case ONCE:

                return pointLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .noneMatch(log -> log.getBehavior() != null && 
                                log.getBehavior().getId().equals(behavior.getId()));
            default:
                return true;
        }

        if (startDate != null) {
            Long count = pointLogRepository.countByUserIdAndBehaviorIdAndDateRange(
                    userId, behavior.getId(), startDate, endDate);
            return count < behavior.getMaxTimesPerFrequency();
        }

        return true;
    }

    private UserGamificationStats getOrCreateUserStats(Long userId) {
        return statsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserGamificationStats newStats = new UserGamificationStats();
                    newStats.setUserId(userId);
                    newStats.setTotalDiligence(0);
                    newStats.setTotalCompetence(0);
                    newStats.setTotalExperience(0);
                    return statsRepository.save(newStats);
                });
    }

    private List<UserAchievementDTO> checkAndUnlockAchievements(Long userId, UserGamificationStats stats) {
        List<UserAchievementDTO> newlyUnlocked = new ArrayList<>();

        List<GamificationAchievementLevel> allLevels = achievementLevelRepository.findAll();

        for (GamificationAchievementLevel level : allLevels) {

            List<UserAchievement> existing = achievementRepository.findByUserIdAndAchievementLevelId(
                    userId, level.getId());
            if (!existing.isEmpty()) {
                continue; 
            }

            boolean canUnlock = false;
            Integer currentPoints = 0;

            if (level.getRequiredPointTypeEnum() != null) {
                switch (level.getRequiredPointTypeEnum()) {
                    case DILIGENCE:
                        currentPoints = stats.getTotalDiligence();
                        break;
                    case COMPETENCE:
                        currentPoints = stats.getTotalCompetence();
                        break;
                    case EXPERIENCE:
                        currentPoints = stats.getTotalExperience();
                        break;
                }
            } else if (level.getRequiredPointTypeId() != null) {

                continue;
            }

            if (currentPoints >= level.getMinPointsRequired()) {
                canUnlock = true;
            }

            if (canUnlock) {

                UserAchievement userAchievement = new UserAchievement();
                userAchievement.setUserId(userId);
                userAchievement.setAchievementLevel(level);
                userAchievement.setIsDisplayed(false);
                achievementRepository.save(userAchievement);

                UserAchievementDTO dto = new UserAchievementDTO();
                dto.setId(userAchievement.getId());
                dto.setUserId(userId);
                dto.setAchievementLevelId(level.getId());
                if (level.getAchievement() != null) {
                    dto.setAchievementName(level.getAchievement().getName());
                }
                dto.setLevelName(level.getLevelName());
                dto.setUnlockedAt(userAchievement.getUnlockedAt());
                dto.setIsDisplayed(userAchievement.getIsDisplayed());
                newlyUnlocked.add(dto);

                sendAchievementUnlockedNotification(userId, level);
            }
        }

        return newlyUnlocked;
    }

    private void sendPointEarnedNotification(Long userId, GamificationBehavior behavior,
                                            Integer diligence, Integer competence, Integer experience) {
        StringBuilder content = new StringBuilder("Bạn đã nhận được ");
        List<String> points = new ArrayList<>();
        
        if (diligence > 0) {
            points.add(diligence + " điểm Chuyên cần");
        }
        if (competence > 0) {
            points.add(competence + " điểm Năng lực");
        }
        if (experience > 0) {
            points.add(experience + " điểm Kinh nghiệm");
        }
        
        content.append(String.join(", ", points));
        content.append(" từ hành vi: ").append(behavior.getName());

        notificationService.createNotification(
                userId,
                null,
                Notification.NotificationType.GAMIFICATION_POINT_EARNED,
                "BEHAVIOR",
                behavior.getId().longValue(),
                "Nhận điểm thưởng",
                content.toString()
        );
    }

    private void sendAchievementUnlockedNotification(Long userId, GamificationAchievementLevel level) {
        String achievementName = level.getAchievement() != null ? 
                level.getAchievement().getName() : "Thành tích";
        String content = String.format("Chúc mừng! Bạn đã đạt được %s - %s", 
                achievementName, level.getLevelName());

        notificationService.createNotification(
                userId,
                null,
                Notification.NotificationType.GAMIFICATION_ACHIEVEMENT_UNLOCKED,
                "ACHIEVEMENT_LEVEL",
                level.getId().longValue(),
                "Đạt thành tích mới",
                content
        );
    }

    @Transactional(readOnly = true)
    public UserGamificationStatsDTO getUserStats(Long userId) {
        UserGamificationStats stats = getOrCreateUserStats(userId);
        UserGamificationStatsDTO dto = new UserGamificationStatsDTO();
        dto.setUserId(stats.getUserId());
        dto.setTotalDiligence(stats.getTotalDiligence());
        dto.setTotalCompetence(stats.getTotalCompetence());
        dto.setTotalExperience(stats.getTotalExperience());
        dto.setUpdatedAt(stats.getUpdatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<UserPointLogDTO> getUserPointLogs(Long userId) {
        List<UserPointLog> logs = pointLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return logs.stream().map(this::mapToPointLogDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserAchievementDTO> getUserAchievements(Long userId) {
        List<UserAchievement> achievements = achievementRepository.findByUserIdOrderByUnlockedAtDesc(userId);
        return achievements.stream().map(this::mapToAchievementDTO).collect(Collectors.toList());
    }

    private UserPointLogDTO mapToPointLogDTO(UserPointLog log) {
        UserPointLogDTO dto = new UserPointLogDTO();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        if (log.getBehavior() != null) {
            dto.setBehaviorId(log.getBehavior().getId());
            dto.setBehaviorCode(log.getBehavior().getCode());
            dto.setBehaviorName(log.getBehavior().getName());
        }
        dto.setPointsDiligenceEarned(log.getPointsDiligenceEarned());
        dto.setPointsCompetenceEarned(log.getPointsCompetenceEarned());
        dto.setPointsExperienceEarned(log.getPointsExperienceEarned());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }

    private UserAchievementDTO mapToAchievementDTO(UserAchievement achievement) {
        UserAchievementDTO dto = new UserAchievementDTO();
        dto.setId(achievement.getId());
        dto.setUserId(achievement.getUserId());
        if (achievement.getAchievementLevel() != null) {
            dto.setAchievementLevelId(achievement.getAchievementLevel().getId());
            if (achievement.getAchievementLevel().getAchievement() != null) {
                dto.setAchievementName(achievement.getAchievementLevel().getAchievement().getName());
            }
            dto.setLevelName(achievement.getAchievementLevel().getLevelName());
        }
        dto.setUnlockedAt(achievement.getUnlockedAt());
        dto.setIsDisplayed(achievement.getIsDisplayed());
        return dto;
    }
}

