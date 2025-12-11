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
    private final GamificationPointTypeRepository pointTypeRepository;
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
            GamificationPointTypeRepository pointTypeRepository,
            NotificationService notificationService,
            @Lazy RankingService rankingService,
            NotificationTemplateService notificationTemplateService,
            UserService userService) {
        this.behaviorRepository = behaviorRepository;
        this.pointLogRepository = pointLogRepository;
        this.statsRepository = statsRepository;
        this.achievementRepository = achievementRepository;
        this.achievementLevelRepository = achievementLevelRepository;
        this.pointTypeRepository = pointTypeRepository;
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

        // Initialize point values - use new behaviorPointTypes if available, otherwise fall back to old fields
        Integer pointsDiligence = 0;
        Integer pointsCompetence = 0;
        Integer pointsExperience = 0;

        // Check if behavior has new point types structure
        if (behavior.getBehaviorPointTypes() != null && !behavior.getBehaviorPointTypes().isEmpty()) {
            // Use new behaviorPointTypes
            for (com.tim.appTim.entity.BehaviorPointType bpt : behavior.getBehaviorPointTypes()) {
                if (bpt.getPointType() != null && bpt.getPoints() != null && bpt.getPoints() > 0) {
                    String pointTypeName = bpt.getPointType().getName().toLowerCase().trim();
                    Integer points = bpt.getPoints();
                    
                    // Map point types to old structure for backward compatibility
                    // Map dựa trên tên point type để lưu vào user_point_logs
                    if (isDiligencePointType(pointTypeName)) {
                        pointsDiligence += points;
                    } else if (isCompetencePointType(pointTypeName)) {
                        pointsCompetence += points;
                    } else if (isExperiencePointType(pointTypeName)) {
                        pointsExperience += points;
                    } else {
                        // For new point types that don't match old ones, we'll add to experience as default
                        // Điều này đảm bảo điểm vẫn được trao và lưu vào user_point_logs
                        pointsExperience += points;
                    }
                }
            }
        } else {
            // Fall back to old fields for backward compatibility
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
                // Xử lý khi dùng enum (DILIGENCE, COMPETENCE, EXPERIENCE)
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
                // Xử lý khi dùng requiredPointTypeId (ID từ bảng gamification_point_types)
                try {
                    GamificationPointType pointType = pointTypeRepository.findById(level.getRequiredPointTypeId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Không tìm thấy point type với ID: " + level.getRequiredPointTypeId()));
                    
                    // Map point type name sang các trường tương ứng trong stats
                    String pointTypeName = pointType.getName().toLowerCase().trim();
                    
                    if (isDiligencePointType(pointTypeName)) {
                        currentPoints = stats.getTotalDiligence();
                    } else if (isCompetencePointType(pointTypeName)) {
                        currentPoints = stats.getTotalCompetence();
                    } else if (isExperiencePointType(pointTypeName)) {
                        currentPoints = stats.getTotalExperience();
                    } else {
                        // Nếu không khớp với bất kỳ loại nào, mặc định dùng Experience
                        // Hoặc có thể skip achievement này nếu muốn strict hơn
                        currentPoints = stats.getTotalExperience();
                    }
                } catch (ResourceNotFoundException e) {
                    // Nếu không tìm thấy point type, skip achievement này
                    System.err.println("Warning: Achievement level " + level.getId() + 
                            " references invalid point type ID: " + level.getRequiredPointTypeId());
                    continue;
                }
            } else {
                // Nếu không có cả enum và pointTypeId, skip achievement này
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
        
        // If behavior has new point types structure, send notifications for each
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
            return; // Exit early if using new structure
        }
        
        // Send separate notifications for each point type if template is configured (old structure)
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
        String levelName = level.getLevelName() != null ? level.getLevelName() : "";

        String fallbackContent = String.format("Chúc mừng! Bạn đã đạt được %s - %s",
                achievementName, levelName);

        String baseTitle = rendered != null ? rendered.getTitle() : "Đạt thành tích mới";
        String baseContent = rendered != null ? rendered.getContent() : fallbackContent;

        // Bảo đảm tiêu đề hiển thị rõ level đạt được
        String title = baseTitle;
        if (levelName != null && !levelName.isBlank() && !baseTitle.toLowerCase().contains(levelName.toLowerCase())) {
            title = baseTitle + " - " + levelName;
        }

        // Nếu nội dung template không chứa level, bổ sung phần mô tả ngắn để phân biệt các cấp
        String content = baseContent;
        if (levelName != null && !levelName.isBlank()
                && !baseContent.toLowerCase().contains(levelName.toLowerCase())) {
            content = baseContent + " (" + achievementName + " - " + levelName + ")";
        }

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

    /**
     * Kiểm tra xem point type có phải là "Chuyên cần" (Diligence) không
     */
    private boolean isDiligencePointType(String pointTypeName) {
        return pointTypeName.equals("chuyên cần") || 
               pointTypeName.equals("diligence") ||
               pointTypeName.contains("chuyên cần") ||
               pointTypeName.contains("diligence");
    }

    /**
     * Kiểm tra xem point type có phải là "Năng lực" (Competence) không
     */
    private boolean isCompetencePointType(String pointTypeName) {
        return pointTypeName.equals("năng lực") || 
               pointTypeName.equals("competence") ||
               pointTypeName.contains("năng lực") ||
               pointTypeName.contains("competence");
    }

    /**
     * Kiểm tra xem point type có phải là "Kinh nghiệm" (Experience) không
     */
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

