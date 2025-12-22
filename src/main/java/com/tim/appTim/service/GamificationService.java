package com.tim.appTim.service;

import com.tim.appTim.dto.*;
import com.tim.appTim.entity.*;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final GamificationAchievementRepository gamificationAchievementRepository;
    private final GamificationPointTypeRepository pointTypeRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final RankingService rankingService;
    private final NotificationTemplateService notificationTemplateService;
    private final UserService userService;

    public GamificationService(
            GamificationBehaviorRepository behaviorRepository,
            UserPointLogRepository pointLogRepository,
            UserGamificationStatsRepository statsRepository,
            UserAchievementRepository achievementRepository,
            GamificationAchievementLevelRepository achievementLevelRepository,
            GamificationAchievementRepository gamificationAchievementRepository,
            GamificationPointTypeRepository pointTypeRepository,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            @Lazy RankingService rankingService,
            NotificationTemplateService notificationTemplateService,
            UserService userService) {
        this.behaviorRepository = behaviorRepository;
        this.pointLogRepository = pointLogRepository;
        this.statsRepository = statsRepository;
        this.achievementRepository = achievementRepository;
        this.achievementLevelRepository = achievementLevelRepository;
        this.gamificationAchievementRepository = gamificationAchievementRepository;
        this.pointTypeRepository = pointTypeRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
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
        Integer pointsDiligence = 0;
        Integer pointsCompetence = 0;
        Integer pointsExperience = 0;

        if (behavior.getBehaviorPointTypes() != null && !behavior.getBehaviorPointTypes().isEmpty()) {
            for (com.tim.appTim.entity.BehaviorPointType bpt : behavior.getBehaviorPointTypes()) {
                if (bpt.getPointType() != null && bpt.getPoints() != null && bpt.getPoints() > 0) {
                    String pointTypeName = bpt.getPointType().getName().toLowerCase().trim();
                    Integer points = bpt.getPoints();

                    if (isDiligencePointType(pointTypeName)) {
                        pointsDiligence += points;
                    } else if (isCompetencePointType(pointTypeName)) {
                        pointsCompetence += points;
                    } else if (isExperiencePointType(pointTypeName)) {
                        pointsExperience += points;
                    } else {
                        pointsExperience += points;
                    }
                }
            }
        } else {
            pointsDiligence = behavior.getPointDiligence() != null ? behavior.getPointDiligence() : 0;
            pointsCompetence = behavior.getPointCompetence() != null ? behavior.getPointCompetence() : 0;
            pointsExperience = behavior.getPointExperience() != null ? behavior.getPointExperience() : 0;
        }

        UserPointLog pointLog = new UserPointLog();
        pointLog.setUserId(userId);
        pointLog.setBehavior(behavior);
        pointLog.setPointsDiligenceEarned(pointsDiligence);
        pointLog.setPointsCompetenceEarned(pointsCompetence);
        pointLog.setPointsExperienceEarned(pointsExperience);
        pointLogRepository.save(pointLog);

        UserGamificationStats stats = getOrCreateUserStats(userId);
        stats.setTotalDiligence(stats.getTotalDiligence() + pointsDiligence);
        stats.setTotalCompetence(stats.getTotalCompetence() + pointsCompetence);
        stats.setTotalExperience(stats.getTotalExperience() + pointsExperience);
        statsRepository.save(stats);

        try {
            rankingService.updateRanking(userId);
        } catch (Exception e) {

            System.err.println("Failed to update ranking for user " + userId + ": " + e.getMessage());
        }

        List<UserAchievementDTO> newlyUnlocked = checkAndUnlockAchievements(userId, stats);

        sendPointEarnedNotification(userId, behavior, 
                pointsDiligence, 
                pointsCompetence, 
                pointsExperience);

        AwardPointsResponse response = new AwardPointsResponse();
        response.setUserId(userId);
        response.setPointsDiligenceEarned(pointsDiligence);
        response.setPointsCompetenceEarned(pointsCompetence);
        response.setPointsExperienceEarned(pointsExperience);
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
        Map<Integer, GamificationAchievementLevel> highestUnlockedByAchievement = new HashMap<>();
        Map<Integer, Boolean> revokedNotificationSent = new HashMap<>();

        List<GamificationAchievementLevel> allLevels = achievementLevelRepository.findAll();

        for (GamificationAchievementLevel level : allLevels) {
            List<UserAchievement> existing = achievementRepository.findByUserIdAndAchievementLevelId(
                    userId, level.getId());
            boolean alreadyHasAchievement = !existing.isEmpty();

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
                try {
                    GamificationPointType pointType = pointTypeRepository.findById(level.getRequiredPointTypeId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Không tìm thấy point type với ID: " + level.getRequiredPointTypeId()));

                    String pointTypeName = pointType.getName().toLowerCase().trim();
                    
                    if (isDiligencePointType(pointTypeName)) {
                        currentPoints = stats.getTotalDiligence();
                    } else if (isCompetencePointType(pointTypeName)) {
                        currentPoints = stats.getTotalCompetence();
                    } else if (isExperiencePointType(pointTypeName)) {
                        currentPoints = stats.getTotalExperience();
                    } else {

                        currentPoints = stats.getTotalExperience();
                    }
                } catch (ResourceNotFoundException e) {
                    System.err.println("Warning: Achievement level " + level.getId() + 
                            " references invalid point type ID: " + level.getRequiredPointTypeId());
                    continue;
                }
            } else {
                continue;
            }

            if (currentPoints >= level.getMinPointsRequired()) {
                canUnlock = true;
            }

            if (canUnlock) {
                if (!alreadyHasAchievement) {
                    UserAchievement userAchievement = new UserAchievement();
                    userAchievement.setUserId(userId);
                    userAchievement.setAchievementLevel(level);
                    userAchievement.setIsDisplayed(true);
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

                    Integer achievementId = level.getAchievement() != null ? level.getAchievement().getId() : null;
                    if (achievementId != null) {
                        GamificationAchievementLevel existingHighest = highestUnlockedByAchievement.get(achievementId);
                        if (existingHighest == null || (level.getMinPointsRequired() != null && 
                            existingHighest.getMinPointsRequired() != null &&
                            level.getMinPointsRequired() > existingHighest.getMinPointsRequired())) {
                            highestUnlockedByAchievement.put(achievementId, level);
                        }
                    }
                } else {
                    boolean needsUpdate = false;
                    for (UserAchievement ua : existing) {
                        if (!Boolean.TRUE.equals(ua.getIsDisplayed())) {
                            ua.setIsDisplayed(true);
                            needsUpdate = true;
                        }
                    }
                    if (needsUpdate) {
                        achievementRepository.saveAll(existing);
                    }

                }
            } else {
                if (alreadyHasAchievement) {
                    for (UserAchievement ua : existing) {
                        ua.setIsDisplayed(false);
                    }
                    achievementRepository.saveAll(existing);

                    Integer achievementId = level.getAchievement() != null ? level.getAchievement().getId() : null;
                    if (achievementId != null && !revokedNotificationSent.containsKey(achievementId)) {
                        sendAchievementRevokedNotification(userId, level, currentPoints);
                        revokedNotificationSent.put(achievementId, true);
                    }
                }
            }
        }

        for (GamificationAchievementLevel highestLevel : highestUnlockedByAchievement.values()) {
            boolean isNewlyUnlocked = newlyUnlocked.stream()
                .anyMatch(dto -> dto.getAchievementLevelId() != null && 
                    dto.getAchievementLevelId().equals(highestLevel.getId()));
            
            if (!isNewlyUnlocked) {
                continue;
            }

            Integer achievementId = highestLevel.getAchievement() != null ? highestLevel.getAchievement().getId() : null;
            boolean hasHigherLevel = false;
            if (achievementId != null && highestLevel.getMinPointsRequired() != null) {
                List<GamificationAchievementLevel> allLevelsOfThisAchievement = achievementLevelRepository.findAll()
                    .stream()
                    .filter(l -> l.getAchievement() != null && l.getAchievement().getId().equals(achievementId))
                    .collect(Collectors.toList());
                
                for (GamificationAchievementLevel otherLevel : allLevelsOfThisAchievement) {
                    if (otherLevel.getMinPointsRequired() != null &&
                        otherLevel.getMinPointsRequired() > highestLevel.getMinPointsRequired()) {
                        List<UserAchievement> existingHigherLevel = achievementRepository.findByUserIdAndAchievementLevelId(
                            userId, otherLevel.getId());
                        if (!existingHigherLevel.isEmpty()) {
                            hasHigherLevel = true;
                            break;
                        }

                        boolean isHigherLevelNewlyUnlocked = newlyUnlocked.stream()
                            .anyMatch(dto -> dto.getAchievementLevelId() != null && 
                                dto.getAchievementLevelId().equals(otherLevel.getId()));
                        if (isHigherLevelNewlyUnlocked) {
                            hasHigherLevel = true;
                            break;
                        }
                    }
                }
            }

            if (!hasHigherLevel) {
                System.out.println(String.format(
                    "[GamificationService] Sending achievement unlocked notification. UserId: %s, AchievementLevelId: %s, LevelName: %s",
                    userId, highestLevel.getId(), highestLevel.getLevelName()));
                sendAchievementUnlockedNotification(userId, highestLevel);
            } else {
                System.out.println(String.format(
                    "[GamificationService] Skipping notification - user has higher level. UserId: %s, AchievementLevelId: %s, LevelName: %s",
                    userId, highestLevel.getId(), highestLevel.getLevelName()));
            }
        }

        return newlyUnlocked;
    }

    private void sendPointEarnedNotification(Long userId, GamificationBehavior behavior,
                                            Integer diligence, Integer competence, Integer experience) {
        Map<String, Object> variables = buildCommonVariables(userId, behavior);

        if (behavior.getBehaviorPointTypes() != null && !behavior.getBehaviorPointTypes().isEmpty()) {
            for (com.tim.appTim.entity.BehaviorPointType bpt : behavior.getBehaviorPointTypes()) {
                if (bpt.getPointType() != null && bpt.getPoints() != null && bpt.getPoints() > 0) {
                    Map<String, Object> bptVariables = new HashMap<>(variables);
                    bptVariables.put("point", bpt.getPoints());
                    bptVariables.put("point_type_name", bpt.getPointType().getName());
                    
                    NotificationTemplate template = bpt.getNotificationTemplate();
                    if (template != null) {
                        NotificationTemplateService.RenderedTemplate rendered = notificationTemplateService.renderById(
                                template.getId(),
                                bptVariables
                        );
                        
                        String fallbackContent = String.format("Bạn đã nhận được %d %s từ hành vi: %s",
                                bpt.getPoints(), bpt.getPointType().getName(), behavior.getName());
                        
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
                }
            }
            return; 
        }

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

        if (level.getNotificationTemplate() != null) {
            rendered = notificationTemplateService.renderById(
                    level.getNotificationTemplate().getId(),
                    variables
            );
        }
        if (rendered == null) {
            rendered = notificationTemplateService.render(
                    "GAMIFICATION_ACHIEVEMENT_UNLOCKED",
                    variables
            );
        }

        String achievementName = level.getAchievement() != null ?
                level.getAchievement().getName() : "Thành tích";
        String levelName = level.getLevelName() != null ? level.getLevelName() : "";

        String fallbackContent = String.format("Chúc mừng! Bạn đã đạt được %s - %s",
                achievementName, levelName);

        String baseTitle = rendered != null ? rendered.getTitle() : "Đạt thành tích mới";
        String baseContent = rendered != null ? rendered.getContent() : fallbackContent;

        String title = baseTitle;
        if (levelName != null && !levelName.isBlank() && !baseTitle.toLowerCase().contains(levelName.toLowerCase())) {
            title = baseTitle + " - " + levelName;
        }

        String content = baseContent;
        if (levelName != null && !levelName.isBlank()
                && !baseContent.toLowerCase().contains(levelName.toLowerCase())) {
            content = baseContent + " (" + achievementName + " - " + levelName + ")";
        }

        String iconUrl = level.getImageUrl();
        if (iconUrl == null || iconUrl.isBlank()) {
            iconUrl = rendered != null ? rendered.getIconUrl() : null;
        }
        if (iconUrl == null && level.getAchievement() != null && level.getAchievement().getImageUrl() != null) {
            iconUrl = level.getAchievement().getImageUrl();
        }

        try {
            List<Notification> existing = notificationRepository.findByReceiverIdAndNotificationTypeAndTargetTypeAndTargetId(
                    userId,
                    Notification.NotificationType.GAMIFICATION_ACHIEVEMENT_UNLOCKED,
                    "ACHIEVEMENT_LEVEL",
                    level.getId().longValue()
            );
            if (!existing.isEmpty()) {
                System.out.println(String.format(
                        "[GamificationService] Skip duplicate achievement notification. UserId: %s, LevelId: %s",
                        userId, level.getId()));
                return;
            }

            NotificationDTO notificationDTO = notificationService.createNotification(
                    userId,
                    null,
                    Notification.NotificationType.GAMIFICATION_ACHIEVEMENT_UNLOCKED,
                    "ACHIEVEMENT_LEVEL",
                    level.getId().longValue(),
                    title,
                    content,
                    iconUrl
            );

            if (notificationDTO == null) {
                System.err.println(String.format(
                    "[GamificationService] Failed to create achievement notification. UserId: %s, AchievementLevelId: %s, Title: %s", 
                    userId, level.getId(), title));
                return;
            }
            
            if (notificationDTO.getId() == null) {
                System.err.println(String.format(
                    "[GamificationService] Achievement notification created but has no ID. UserId: %s, AchievementLevelId: %s, Title: %s, DTO: %s", 
                    userId, level.getId(), title, notificationDTO));
                return;
            }
            
            System.out.println(String.format(
                "[GamificationService] Successfully created achievement notification. ID: %s, UserId: %s, AchievementLevelId: %s, Title: %s", 
                notificationDTO.getId(), userId, level.getId(), title));
        } catch (Exception e) {
            System.err.println(String.format(
                "[GamificationService] Error creating achievement notification. UserId: %s, AchievementLevelId: %s, Title: %s, Error: %s", 
                userId, level.getId(), title, e.getMessage()));
            e.printStackTrace();
        }
    }

    private void sendAchievementRevokedNotification(Long userId,
                                                    GamificationAchievementLevel level,
                                                    Integer currentPoints) {
        String achievementName = level.getAchievement() != null
                ? level.getAchievement().getName()
                : "Thành tích";
        String levelName = level.getLevelName() != null ? level.getLevelName() : "";
        Integer requiredPoints = level.getMinPointsRequired();

        String title = "Bạn không còn đủ điều kiện cho huy hiệu";
        if (levelName != null && !levelName.isBlank()) {
            title = title + " - " + levelName;
        }

        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("Điểm hiện tại của bạn cho loại điểm này là ")
                .append(currentPoints != null ? currentPoints : 0)
                .append(", nhỏ hơn mức yêu cầu mới ");
        if (requiredPoints != null) {
            contentBuilder.append(requiredPoints);
        }
        contentBuilder.append(" để đạt huy hiệu ");
        contentBuilder.append(achievementName);
        if (levelName != null && !levelName.isBlank()) {
            contentBuilder.append(" - ").append(levelName);
        }

        String iconUrl = level.getImageUrl();
        if ((iconUrl == null || iconUrl.isBlank()) &&
                level.getAchievement() != null &&
                level.getAchievement().getImageUrl() != null) {
            iconUrl = level.getAchievement().getImageUrl();
        }

        try {
            Integer achievementId = level.getAchievement() != null ? level.getAchievement().getId() : null;
            if (achievementId != null && requiredPoints != null) {
                List<Notification> existingNotifications = notificationRepository.findByReceiverIdAndNotificationTypeAndTargetTypeAndTargetId(
                        userId,
                        Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        "ACHIEVEMENT",
                        achievementId.longValue()
                );
                boolean hasRevokedNotificationWithSamePoints = existingNotifications.stream()
                        .anyMatch(n -> {
                            if (n.getTitle() == null || !n.getTitle().contains("không còn đủ điều kiện")) {
                                return false;
                            }
                            String content = n.getContent();
                            if (content == null) return false;

                            String searchPattern = "mức yêu cầu mới ";
                            int index = content.indexOf(searchPattern);
                            if (index == -1) return false;
                            
                            int startIndex = index + searchPattern.length();
                            int endIndex = startIndex;
                            while (endIndex < content.length() && Character.isDigit(content.charAt(endIndex))) {
                                endIndex++;
                            }
                            
                            if (endIndex > startIndex) {
                                try {
                                    Integer existingRequiredPoints = Integer.parseInt(content.substring(startIndex, endIndex));

                                    return existingRequiredPoints.equals(requiredPoints);
                                } catch (NumberFormatException e) {
                                    return false;
                                }
                            }
                            return false;
                        });
                
                if (hasRevokedNotificationWithSamePoints) {
                    System.out.println(String.format(
                            "[GamificationService] Skipping revoked notification - already sent for this required points. UserId: %s, AchievementId: %s, RequiredPoints: %d",
                            userId, achievementId, requiredPoints));
                    return;
                }
            }

            notificationService.createNotification(
                    userId,
                    null,
                    Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    "ACHIEVEMENT", 
                    achievementId != null ? achievementId.longValue() : level.getId().longValue(),
                    title,
                    contentBuilder.toString(),
                    iconUrl
            );
        } catch (Exception e) {
            System.err.println(String.format(
                    "[GamificationService] Error creating achievement revoked notification. UserId: %s, AchievementLevelId: %s, Error: %s",
                    userId, level.getId(), e.getMessage()));
            e.printStackTrace();
        }
    }

    private boolean isDiligencePointType(String pointTypeName) {
        return pointTypeName.equals("chuyên cần") || 
               pointTypeName.equals("diligence") ||
               pointTypeName.contains("chuyên cần") ||
               pointTypeName.contains("diligence");
    }

    private boolean isCompetencePointType(String pointTypeName) {
        return pointTypeName.equals("năng lực") || 
               pointTypeName.equals("competence") ||
               pointTypeName.contains("năng lực") ||
               pointTypeName.contains("competence");
    }

    private boolean isExperiencePointType(String pointTypeName) {
        return pointTypeName.equals("kinh nghiệm") || 
               pointTypeName.equals("experience") ||
               pointTypeName.contains("kinh nghiệm") ||
               pointTypeName.contains("experience");
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
    public Page<UserPointLogDTO> getUserPointLogs(Long userId, Pageable pageable) {
        return pointLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToPointLogDTO);
    }

    @Transactional(readOnly = true)
    public List<UserAchievementDTO> getUserAchievements(Long userId) {
        List<UserAchievement> achievements = achievementRepository.findByUserIdOrderByUnlockedAtDesc(userId);
        return achievements.stream().map(this::mapToAchievementDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserAchievementDTO> getUserAchievements(Long userId, Pageable pageable) {
        return achievementRepository.findByUserIdOrderByUnlockedAtDesc(userId, pageable)
                .map(this::mapToAchievementDTO);
    }


    @Transactional
    public void checkAchievementsForAllUsers() {
        System.out.println("=== Starting checkAchievementsForAllUsers ===");
        List<UserGamificationStats> allStats = statsRepository.findAll();
        System.out.println("Found " + allStats.size() + " users with stats");
        
        for (UserGamificationStats stats : allStats) {
            try {
                System.out.println("Checking achievements for user " + stats.getUserId() + 
                    " (Diligence: " + stats.getTotalDiligence() + 
                    ", Competence: " + stats.getTotalCompetence() + 
                    ", Experience: " + stats.getTotalExperience() + ")");
                List<UserAchievementDTO> unlocked = checkAndUnlockAchievements(stats.getUserId(), stats);
                if (!unlocked.isEmpty()) {
                    System.out.println("Unlocked " + unlocked.size() + " achievements for user " + stats.getUserId());
                    for (UserAchievementDTO dto : unlocked) {
                        System.out.println("  - " + dto.getAchievementName() + " - " + dto.getLevelName());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error checking achievements for user " + stats.getUserId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("=== Finished checkAchievementsForAllUsers ===");
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

    @Transactional
    public List<AchievementWithStatusDTO> getAllAchievementsWithUserStatus(Long userId) {
        UserGamificationStats stats = getOrCreateUserStats(userId);

        try {
            checkAndUnlockAchievements(userId, stats);
        } catch (Exception e) {
            System.err.println("Error checking achievements in getAllAchievementsWithUserStatus: " + e.getMessage());
            e.printStackTrace();
        }

        List<GamificationAchievementLevel> allLevels = achievementLevelRepository.findAll();
        System.out.println(String.format("[GamificationService] getAllAchievementsWithUserStatus - Total levels in DB: %d", allLevels.size()));

        List<UserAchievement> userAchievements = achievementRepository.findDisplayedByUserId(userId);
        Map<Integer, UserAchievement> userAchievementMap = new HashMap<>();
        Map<Integer, LocalDateTime> unlockedAtMap = new HashMap<>();
        for (UserAchievement ua : userAchievements) {
            if (ua.getAchievementLevel() != null) {
                Integer levelId = ua.getAchievementLevel().getId();
                userAchievementMap.put(levelId, ua);
                unlockedAtMap.put(levelId, ua.getUnlockedAt());
            }
        }

        long totalUsers = statsRepository.count();
        if (totalUsers == 0) totalUsers = 1; 

        Map<Integer, List<GamificationAchievementLevel>> levelsByAchievement = new HashMap<>();
        Map<Integer, GamificationAchievement> achievementMap = new HashMap<>();
        
        for (GamificationAchievementLevel level : allLevels) {
            if (level.getAchievement() != null) {
                Integer achievementId = level.getAchievement().getId();
                levelsByAchievement.computeIfAbsent(achievementId, k -> new ArrayList<>()).add(level);
                achievementMap.put(achievementId, level.getAchievement());
            }
        }

        for (List<GamificationAchievementLevel> levels : levelsByAchievement.values()) {
            levels.sort((a, b) -> {
                Integer aPoints = a.getMinPointsRequired() != null ? a.getMinPointsRequired() : 0;
                Integer bPoints = b.getMinPointsRequired() != null ? b.getMinPointsRequired() : 0;
                return aPoints.compareTo(bPoints);
            });
        }
        
        List<AchievementWithStatusDTO> result = new ArrayList<>();
        int processedCount = 0;

        for (Map.Entry<Integer, List<GamificationAchievementLevel>> entry : levelsByAchievement.entrySet()) {
            Integer achievementId = entry.getKey();
            List<GamificationAchievementLevel> levels = entry.getValue();
            GamificationAchievement achievement = achievementMap.get(achievementId);
            
            if (achievement == null || levels.isEmpty()) {
                continue; 
            }
            
            try {
                
                AchievementWithStatusDTO dto = new AchievementWithStatusDTO();
                dto.setAchievementId(achievement.getId());
                dto.setAchievementName(achievement.getName());
                dto.setAchievementImageUrl(achievement.getImageUrl());

            GamificationAchievementLevel highestUnlockedLevel = null;
            GamificationAchievementLevel nextLevel = null;
            Integer highestUnlockedLevelId = null;
            String highestUnlockedLevelName = null;
            LocalDateTime unlockedAt = null;
            String highestUnlockedLevelImageUrl = null;
            GamificationAchievementLevel firstLevel = levels.get(0);
            Integer currentPoints = 0;
            String pointTypeName = null;
            
            if (firstLevel.getRequiredPointTypeEnum() != null) {
                switch (firstLevel.getRequiredPointTypeEnum()) {
                    case DILIGENCE:
                        currentPoints = stats.getTotalDiligence();
                        pointTypeName = "Chuyên cần";
                        break;
                    case COMPETENCE:
                        currentPoints = stats.getTotalCompetence();
                        pointTypeName = "Năng lực";
                        break;
                    case EXPERIENCE:
                        currentPoints = stats.getTotalExperience();
                        pointTypeName = "Kinh nghiệm";
                        break;
                }
            } else if (firstLevel.getRequiredPointTypeId() != null) {
                try {
                    GamificationPointType pointType = pointTypeRepository.findById(firstLevel.getRequiredPointTypeId())
                            .orElse(null);
                    if (pointType != null) {
                        pointTypeName = pointType.getName();
                        String ptName = pointType.getName().toLowerCase().trim();
                        if (isDiligencePointType(ptName)) {
                            currentPoints = stats.getTotalDiligence();
                        } else if (isCompetencePointType(ptName)) {
                            currentPoints = stats.getTotalCompetence();
                        } else if (isExperiencePointType(ptName)) {
                            currentPoints = stats.getTotalExperience();
                        } else {
                            currentPoints = stats.getTotalExperience(); 
                        }
                    }
                } catch (Exception e) {
                    currentPoints = stats.getTotalExperience();
                }
            }
            
            dto.setCurrentPoints(currentPoints);
            for (GamificationAchievementLevel level : levels) {
                Integer levelId = level.getId();
                boolean isUnlockedInDB = userAchievementMap.containsKey(levelId);
                boolean isUnlocked = isUnlockedInDB;
                
                if (isUnlocked && (highestUnlockedLevel == null || 
                    (level.getMinPointsRequired() != null && highestUnlockedLevel.getMinPointsRequired() != null &&
                     level.getMinPointsRequired() > highestUnlockedLevel.getMinPointsRequired()))) {
                    highestUnlockedLevel = level;
                    highestUnlockedLevelId = levelId;
                    highestUnlockedLevelName = level.getLevelName();
                    unlockedAt = isUnlockedInDB ? unlockedAtMap.get(levelId) : null;
                    highestUnlockedLevelImageUrl = level.getImageUrl() != null ? level.getImageUrl() : achievement.getImageUrl();
                }

                if (nextLevel == null && !isUnlocked) {
                    nextLevel = level;
                }
            }

            if (highestUnlockedLevel != null) {
                dto.setHighestUnlockedLevelId(highestUnlockedLevelId);
                dto.setHighestUnlockedLevelName(highestUnlockedLevelName);
                dto.setUnlockedAt(unlockedAt);
                dto.setHighestUnlockedLevelImageUrl(highestUnlockedLevelImageUrl);
                dto.setIsUnlocked(true);
                dto.setMinPointsRequired(highestUnlockedLevel.getMinPointsRequired());
            } else {
                dto.setIsUnlocked(false);
                if (nextLevel != null) {
                    dto.setMinPointsRequired(nextLevel.getMinPointsRequired());
                }
            }

            if (nextLevel != null) {
                dto.setNextLevelId(nextLevel.getId());
                dto.setNextLevelName(nextLevel.getLevelName());
                dto.setNextLevelMinPoints(nextLevel.getMinPointsRequired());
                dto.setNextLevelImageUrl(nextLevel.getImageUrl() != null ? nextLevel.getImageUrl() : achievement.getImageUrl());
            }

            GamificationAchievementLevel maxLevel = levels.get(levels.size() - 1);
            dto.setMaxPointsRequired(maxLevel.getMinPointsRequired());

            if (dto.getIsUnlocked() && highestUnlockedLevel != null) {
                dto.setProgressPercentage(100.0);
            } else if (nextLevel != null && nextLevel.getMinPointsRequired() != null) {
                Integer minForNext = nextLevel.getMinPointsRequired();
                Integer minForPrev = 0;
                for (int i = 0; i < levels.size(); i++) {
                    if (levels.get(i).getId().equals(nextLevel.getId()) && i > 0) {
                        minForPrev = levels.get(i - 1).getMinPointsRequired() != null ? 
                                     levels.get(i - 1).getMinPointsRequired() : 0;
                        break;
                    }
                }
                if (minForNext > minForPrev) {
                    double progress = ((double)(currentPoints - minForPrev) / (minForNext - minForPrev)) * 100.0;
                    dto.setProgressPercentage(Math.max(0.0, Math.min(100.0, progress)));
                } else {
                    dto.setProgressPercentage(0.0);
                }
            } else {
                dto.setProgressPercentage(100.0); 
            }

            boolean allUnlocked = true;
            for (GamificationAchievementLevel level : levels) {
                if (!userAchievementMap.containsKey(level.getId())) {
                    allUnlocked = false;
                    break;
                }
            }
            dto.setIsFullyUnlocked(allUnlocked);

            long usersWithAchievement = achievementRepository.countDistinctUsersByAchievementId(achievement.getId());
            double rarity = (usersWithAchievement * 100.0) / totalUsers;
            dto.setRarityPercentage(rarity);

            if (pointTypeName != null && nextLevel != null) {
                dto.setDescription(String.format("Đạt %d điểm %s", nextLevel.getMinPointsRequired(), pointTypeName));
            } else if (highestUnlockedLevel != null && pointTypeName != null) {
                dto.setDescription(String.format("Đạt %d điểm %s", highestUnlockedLevel.getMinPointsRequired(), pointTypeName));
            } else {
                dto.setDescription(achievement.getName());
            }

            List<AchievementWithStatusDTO.AchievementLevelInfoDTO> levelInfoList = new ArrayList<>();
            for (GamificationAchievementLevel level : levels) {
                AchievementWithStatusDTO.AchievementLevelInfoDTO levelInfo = 
                    new AchievementWithStatusDTO.AchievementLevelInfoDTO();
                levelInfo.setLevelId(level.getId());
                levelInfo.setLevelName(level.getLevelName());
                levelInfo.setMinPointsRequired(level.getMinPointsRequired());
                levelInfo.setImageUrl(level.getImageUrl() != null ? level.getImageUrl() : achievement.getImageUrl());
                levelInfo.setIsUnlocked(userAchievementMap.containsKey(level.getId()));
                if (userAchievementMap.containsKey(level.getId())) {
                    levelInfo.setUnlockedAt(unlockedAtMap.get(level.getId()));
                }
                levelInfoList.add(levelInfo);
            }
            dto.setLevels(levelInfoList);
            
                result.add(dto);
                processedCount++;
            } catch (Exception e) {
                System.err.println(String.format(
                        "[GamificationService] Error processing achievement %d: %s", 
                        achievementId,
                        e.getMessage()));
                e.printStackTrace();
            }
        }
        
        System.out.println(String.format(
                "[GamificationService] getAllAchievementsWithUserStatus - Processed: %d achievements with levels, Result size: %d",
                processedCount, result.size()));
        
        return result;
    }
}
