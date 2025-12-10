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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.tim.appTim.entity.User;

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
    private final NotificationTemplateService notificationTemplateService;
    private final UserService userService;

    public GamificationService(
            GamificationBehaviorRepository behaviorRepository,
            UserPointLogRepository pointLogRepository,
            UserGamificationStatsRepository statsRepository,
            UserAchievementRepository achievementRepository,
            GamificationAchievementLevelRepository achievementLevelRepository,
            NotificationService notificationService,
            @Lazy RankingService rankingService,
            NotificationTemplateService notificationTemplateService,
            UserService userService) {
        this.behaviorRepository = behaviorRepository;
        this.pointLogRepository = pointLogRepository;
        this.statsRepository = statsRepository;
        this.achievementRepository = achievementRepository;
        this.achievementLevelRepository = achievementLevelRepository;
        this.notificationService = notificationService;
        this.rankingService = rankingService;
        this.notificationTemplateService = notificationTemplateService;
        this.userService = userService;
    }

    public AwardPointsResponse awardPoints(Long userId, Integer behaviorId) {
        GamificationBehavior behavior = behaviorRepository.findById(behaviorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành vi với id: " + behaviorId));
        return awardPointsInternal(userId, behavior);
    }

    private AwardPointsResponse awardPointsInternal(Long userId, GamificationBehavior behavior) {

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
        Map<String, Object> variables = buildCommonVariables(userId, behavior);
        
        // Send separate notifications for each point type if template is configured
        if (diligence > 0 && behavior.getNotificationTemplateDiligence() != null) {
            variables.put("point", diligence);
            variables.put("point_type_name", "điểm Chuyên cần");
            NotificationTemplateService.RenderedTemplate rendered = notificationTemplateService.renderById(
                    behavior.getNotificationTemplateDiligence().getId(),
                    variables
            );
            
            String fallbackContent = String.format("Bạn đã nhận được %d điểm Chuyên cần từ hành vi: %s",
                    diligence, behavior.getName());
            
            String title = rendered != null ? rendered.getTitle() : "Nhận điểm thưởng";
            String content = rendered != null ? rendered.getContent() : fallbackContent;
            
            notificationService.createNotification(
                    userId,
                    null,
                    Notification.NotificationType.GAMIFICATION_POINT_EARNED,
                    "BEHAVIOR",
                    behavior.getId().longValue(),
                    title,
                    content,
                    rendered != null ? rendered.getIconUrl() : null
            );
        }
        
        if (competence > 0 && behavior.getNotificationTemplateCompetence() != null) {
            variables.put("point", competence);
            variables.put("point_type_name", "điểm Năng lực");
            NotificationTemplateService.RenderedTemplate rendered = notificationTemplateService.renderById(
                    behavior.getNotificationTemplateCompetence().getId(),
                    variables
            );
            
            String fallbackContent = String.format("Bạn đã nhận được %d điểm Năng lực từ hành vi: %s",
                    competence, behavior.getName());
            
            String title = rendered != null ? rendered.getTitle() : "Nhận điểm thưởng";
            String content = rendered != null ? rendered.getContent() : fallbackContent;
            
            notificationService.createNotification(
                    userId,
                    null,
                    Notification.NotificationType.GAMIFICATION_POINT_EARNED,
                    "BEHAVIOR",
                    behavior.getId().longValue(),
                    title,
                    content,
                    rendered != null ? rendered.getIconUrl() : null
            );
        }
        
        if (experience > 0 && behavior.getNotificationTemplateExperience() != null) {
            variables.put("point", experience);
            variables.put("point_type_name", "điểm Kinh nghiệm");
            NotificationTemplateService.RenderedTemplate rendered = notificationTemplateService.renderById(
                    behavior.getNotificationTemplateExperience().getId(),
                    variables
            );
            
            String fallbackContent = String.format("Bạn đã nhận được %d điểm Kinh nghiệm từ hành vi: %s",
                    experience, behavior.getName());
            
            String title = rendered != null ? rendered.getTitle() : "Nhận điểm thưởng";
            String content = rendered != null ? rendered.getContent() : fallbackContent;
            
            notificationService.createNotification(
                    userId,
                    null,
                    Notification.NotificationType.GAMIFICATION_POINT_EARNED,
                    "BEHAVIOR",
                    behavior.getId().longValue(),
                    title,
                    content,
                    rendered != null ? rendered.getIconUrl() : null
            );
        }
        
        // Fallback: if no templates are configured, send one combined notification using default template
        boolean hasAnyTemplate = (diligence > 0 && behavior.getNotificationTemplateDiligence() != null) ||
                                 (competence > 0 && behavior.getNotificationTemplateCompetence() != null) ||
                                 (experience > 0 && behavior.getNotificationTemplateExperience() != null);
        
        if (!hasAnyTemplate && (diligence > 0 || competence > 0 || experience > 0)) {
            variables.put("point", diligence + competence + experience);
            variables.put("point_type_name", "điểm thưởng");
            
            NotificationTemplateService.RenderedTemplate rendered = notificationTemplateService.render(
                    "GAMIFICATION_POINT_EARNED",
                    variables
            );
            
            StringBuilder fallbackContent = new StringBuilder("Bạn đã nhận được ");
            List<String> points = new ArrayList<>();
            if (diligence > 0) points.add(diligence + " điểm Chuyên cần");
            if (competence > 0) points.add(competence + " điểm Năng lực");
            if (experience > 0) points.add(experience + " điểm Kinh nghiệm");
            fallbackContent.append(String.join(", ", points));
            fallbackContent.append(" từ hành vi: ").append(behavior.getName());
            
            String title = rendered != null ? rendered.getTitle() : "Nhận điểm thưởng";
            String content = rendered != null ? rendered.getContent() : fallbackContent.toString();
            
            notificationService.createNotification(
                    userId,
                    null,
                    Notification.NotificationType.GAMIFICATION_POINT_EARNED,
                    "BEHAVIOR",
                    behavior.getId().longValue(),
                    title,
                    content,
                    rendered != null ? rendered.getIconUrl() : null
            );
        }
    }

    private void sendAchievementUnlockedNotification(Long userId, GamificationAchievementLevel level) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_fullname", getUserFullNameSafe(userId));
        variables.put("achievement_name", level.getAchievement() != null ? level.getAchievement().getName() : "Thành tích");
        variables.put("level_name", level.getLevelName());

        NotificationTemplateService.RenderedTemplate rendered = null;
        
        // Use template from achievement level if configured
        if (level.getNotificationTemplate() != null) {
            rendered = notificationTemplateService.renderById(
                    level.getNotificationTemplate().getId(),
                    variables
            );
        }
        
        // Fallback to default template if no template is configured
        if (rendered == null) {
            rendered = notificationTemplateService.render(
                    "GAMIFICATION_ACHIEVEMENT_UNLOCKED",
                    variables
            );
        }

        String achievementName = level.getAchievement() != null ?
                level.getAchievement().getName() : "Thành tích";
        String fallbackContent = String.format("Chúc mừng! Bạn đã đạt được %s - %s",
                achievementName, level.getLevelName());

        String title = rendered != null ? rendered.getTitle() : "Đạt thành tích mới";
        String content = rendered != null ? rendered.getContent() : fallbackContent;

        notificationService.createNotification(
                userId,
                null,
                Notification.NotificationType.GAMIFICATION_ACHIEVEMENT_UNLOCKED,
                "ACHIEVEMENT_LEVEL",
                level.getId().longValue(),
                title,
                content,
                rendered != null ? rendered.getIconUrl() : null
        );
    }

    private Map<String, Object> buildCommonVariables(Long userId, GamificationBehavior behavior) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_fullname", getUserFullNameSafe(userId));
        variables.put("activity_name", behavior != null ? behavior.getName() : "");
        variables.put("behavior_name", behavior != null ? behavior.getName() : "");
        return variables;
    }

    private String getUserFullNameSafe(Long userId) {
        try {
            User user = userService.findById(userId);
            String first = user.getFirstName() != null ? user.getFirstName() : "";
            String last = user.getLastName() != null ? user.getLastName() : "";
            String full = (first + " " + last).trim();
            return full.isEmpty() ? user.getUsername() : full;
        } catch (Exception e) {
            return "";
        }
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

