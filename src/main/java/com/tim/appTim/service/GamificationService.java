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
        Map<Integer, GamificationAchievementLevel> highestUnlockedByAchievement = new HashMap<>();
        // Map để track các achievement đã gửi thông báo revoked (tránh spam)
        Map<Integer, Boolean> revokedNotificationSent = new HashMap<>();

        List<GamificationAchievementLevel> allLevels = achievementLevelRepository.findAll();

        for (GamificationAchievementLevel level : allLevels) {
            // Lấy danh sách achievement hiện có của user cho level này (nếu có)
            List<UserAchievement> existing = achievementRepository.findByUserIdAndAchievementLevelId(
                    userId, level.getId());
            boolean alreadyHasAchievement = !existing.isEmpty();

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

            // ================== XỬ LÝ 3 TRƯỜNG HỢP ==================
            if (canUnlock) {
                // Trường hợp 1 + 2:
                // - Nếu user CHƯA có achievement level này -> tạo mới + gửi thông báo (trường hợp 1)
                // - Nếu user ĐÃ có -> đảm bảo isDisplayed = true, không tạo lại, không gửi lại thông báo (trường hợp 2)
                if (!alreadyHasAchievement) {
                    // Trường hợp 1: Tạo mới achievement với isDisplayed = true vì đã đủ điều kiện
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

                    // Thêm vào highestUnlockedByAchievement để sau đó chỉ gửi thông báo cho level cao nhất
                    // của mỗi achievement được unlock mới trong lần check này
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
                    // Trường hợp 2: User đã có achievement, đảm bảo isDisplayed = true
                    // KHÔNG gửi lại thông báo (đây là điểm quan trọng)
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
                    // KHÔNG thêm vào highestUnlockedByAchievement - không gửi thông báo
                }
            } else {
                // Trường hợp 3: user TRƯỚC ĐÃ có achievement này nhưng hiện tại không còn đủ điểm
                if (alreadyHasAchievement) {
                    // Ẩn các bản ghi hiện tại để FE không hiển thị nữa
                    for (UserAchievement ua : existing) {
                        ua.setIsDisplayed(false);
                    }
                    achievementRepository.saveAll(existing);

                    // QUAN TRỌNG: Chỉ gửi 1 thông báo revoked cho mỗi achievement (không phải mỗi level)
                    // Lấy achievementId để check xem đã gửi thông báo cho achievement này chưa
                    Integer achievementId = level.getAchievement() != null ? level.getAchievement().getId() : null;
                    if (achievementId != null && !revokedNotificationSent.containsKey(achievementId)) {
                        // Gửi thông báo revoked (KHÔNG phải unlocked)
                        sendAchievementRevokedNotification(userId, level, currentPoints);
                        // Đánh dấu đã gửi thông báo cho achievement này
                        revokedNotificationSent.put(achievementId, true);
                    }
                }
            }
        }

        // Chỉ gửi thông báo cho các achievement MỚI được unlock (trong newlyUnlocked)
        // Và chỉ gửi cho level cao nhất của mỗi achievement
        for (GamificationAchievementLevel highestLevel : highestUnlockedByAchievement.values()) {
            // Kiểm tra xem level này có trong newlyUnlocked không (đảm bảo chỉ gửi cho achievement MỚI)
            boolean isNewlyUnlocked = newlyUnlocked.stream()
                .anyMatch(dto -> dto.getAchievementLevelId() != null && 
                    dto.getAchievementLevelId().equals(highestLevel.getId()));
            
            if (!isNewlyUnlocked) {
                // Không phải achievement mới được unlock, bỏ qua
                continue;
            }
            
            // Kiểm tra xem user đã có level nào cao hơn của cùng achievement này chưa
            // (bao gồm cả trong database và trong newlyUnlocked của cùng lần check này)
            Integer achievementId = highestLevel.getAchievement() != null ? highestLevel.getAchievement().getId() : null;
            boolean hasHigherLevel = false;
            if (achievementId != null && highestLevel.getMinPointsRequired() != null) {
                // Kiểm tra trong database
                List<GamificationAchievementLevel> allLevelsOfThisAchievement = achievementLevelRepository.findAll()
                    .stream()
                    .filter(l -> l.getAchievement() != null && l.getAchievement().getId().equals(achievementId))
                    .collect(Collectors.toList());
                
                for (GamificationAchievementLevel otherLevel : allLevelsOfThisAchievement) {
                    if (otherLevel.getMinPointsRequired() != null &&
                        otherLevel.getMinPointsRequired() > highestLevel.getMinPointsRequired()) {
                        // Kiểm tra trong database
                        List<UserAchievement> existingHigherLevel = achievementRepository.findByUserIdAndAchievementLevelId(
                            userId, otherLevel.getId());
                        if (!existingHigherLevel.isEmpty()) {
                            hasHigherLevel = true;
                            break;
                        }
                        
                        // Kiểm tra trong newlyUnlocked của cùng lần check này
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
            
            // Chỉ gửi thông báo nếu: (1) đây là achievement MỚI được unlock, và (2) không có level cao hơn
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

        String iconUrl = level.getImageUrl();
        if (iconUrl == null || iconUrl.isBlank()) {
            iconUrl = rendered != null ? rendered.getIconUrl() : null;
        }
        if (iconUrl == null && level.getAchievement() != null && level.getAchievement().getImageUrl() != null) {
            iconUrl = level.getAchievement().getImageUrl();
        }

        try {
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
            
            // Validate notification was created successfully
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
            // Don't throw - we don't want notification failure to break achievement unlocking
        }
    }

    /**
     * Gửi thông báo khi người dùng KHÔNG còn đủ điều kiện giữ huy hiệu
     * (ví dụ admin tăng điểm tối thiểu vượt quá điểm hiện tại của user).
     */
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
            // QUAN TRỌNG: Check duplicate notification trước khi gửi
            // Sử dụng achievementId làm targetId để check duplicate cho toàn bộ achievement (không phải từng level)
            Integer achievementId = level.getAchievement() != null ? level.getAchievement().getId() : null;
            if (achievementId != null) {
                // Check xem đã có thông báo revoked cho achievement này chưa (trong vòng 1 giờ gần đây)
                List<Notification> existingNotifications = notificationRepository.findByReceiverIdAndNotificationTypeAndTargetTypeAndTargetId(
                        userId,
                        Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        "ACHIEVEMENT",
                        achievementId.longValue()
                );
                
                // Kiểm tra xem có thông báo revoked gần đây không (trong vòng 1 giờ)
                boolean hasRecentRevokedNotification = existingNotifications.stream()
                        .anyMatch(n -> n.getTitle() != null && 
                                n.getTitle().contains("không còn đủ điều kiện") &&
                                n.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusHours(1)));
                
                if (hasRecentRevokedNotification) {
                    System.out.println(String.format(
                            "[GamificationService] Skipping revoked notification - already sent recently. UserId: %s, AchievementId: %s",
                            userId, achievementId));
                    return;
                }
            }
            
            // Gửi thông báo với targetType = "ACHIEVEMENT" và targetId = achievementId để dễ check duplicate
            notificationService.createNotification(
                    userId,
                    null,
                    Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    "ACHIEVEMENT", // Dùng ACHIEVEMENT thay vì ACHIEVEMENT_LEVEL để check duplicate theo achievement
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

    /**
     * Lấy danh sách tất cả achievements với trạng thái unlock của user cụ thể.
     * Bao gồm thông tin về progress, level đã unlock, level tiếp theo, và rarity.
     * Tự động check và unlock achievements nếu user đủ điều kiện nhưng chưa có trong DB.
     */
    @Transactional
    public List<AchievementWithStatusDTO> getAllAchievementsWithUserStatus(Long userId) {
        // Lấy stats của user
        UserGamificationStats stats = getOrCreateUserStats(userId);
        
        // QUAN TRỌNG: Check và unlock achievements nếu user đủ điều kiện nhưng chưa có trong DB
        // Điều này đảm bảo achievements được unlock ngay khi user đủ điểm, kể cả khi chưa award points lần nào
        try {
            checkAndUnlockAchievements(userId, stats);
        } catch (Exception e) {
            System.err.println("Error checking achievements in getAllAchievementsWithUserStatus: " + e.getMessage());
            e.printStackTrace();
            // Không throw - tiếp tục load achievements từ DB
        }
        
        // QUAN TRỌNG: Logic mới - Lấy TẤT CẢ achievement levels, nhóm theo achievement
        // Bỏ qua achievements không có levels
        
        // Lấy tất cả achievement levels, sắp xếp theo achievement_id và minPointsRequired
        List<GamificationAchievementLevel> allLevels = achievementLevelRepository.findAll();
        System.out.println(String.format("[GamificationService] getAllAchievementsWithUserStatus - Total levels in DB: %d", allLevels.size()));
        
        // Lấy tất cả user achievements của user này (chỉ lấy những cái isDisplayed = true)
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
        
        // Tính tổng số user để tính rarity
        long totalUsers = statsRepository.count();
        if (totalUsers == 0) totalUsers = 1; // Tránh chia cho 0
        
        // Nhóm levels theo achievement_id
        Map<Integer, List<GamificationAchievementLevel>> levelsByAchievement = new HashMap<>();
        Map<Integer, GamificationAchievement> achievementMap = new HashMap<>();
        
        for (GamificationAchievementLevel level : allLevels) {
            if (level.getAchievement() != null) {
                Integer achievementId = level.getAchievement().getId();
                levelsByAchievement.computeIfAbsent(achievementId, k -> new ArrayList<>()).add(level);
                achievementMap.put(achievementId, level.getAchievement());
            }
        }
        
        // Sắp xếp levels trong mỗi achievement theo minPointsRequired tăng dần
        for (List<GamificationAchievementLevel> levels : levelsByAchievement.values()) {
            levels.sort((a, b) -> {
                Integer aPoints = a.getMinPointsRequired() != null ? a.getMinPointsRequired() : 0;
                Integer bPoints = b.getMinPointsRequired() != null ? b.getMinPointsRequired() : 0;
                return aPoints.compareTo(bPoints);
            });
        }
        
        List<AchievementWithStatusDTO> result = new ArrayList<>();
        int processedCount = 0;
        
        // Xử lý từng achievement có levels
        for (Map.Entry<Integer, List<GamificationAchievementLevel>> entry : levelsByAchievement.entrySet()) {
            Integer achievementId = entry.getKey();
            List<GamificationAchievementLevel> levels = entry.getValue();
            GamificationAchievement achievement = achievementMap.get(achievementId);
            
            if (achievement == null || levels.isEmpty()) {
                continue; // Bỏ qua nếu không có achievement hoặc không có levels
            }
            
            try {
                
                AchievementWithStatusDTO dto = new AchievementWithStatusDTO();
                dto.setAchievementId(achievement.getId());
                dto.setAchievementName(achievement.getName());
                dto.setAchievementImageUrl(achievement.getImageUrl());
                
                // Tìm level cao nhất user đã unlock và level tiếp theo
            GamificationAchievementLevel highestUnlockedLevel = null;
            GamificationAchievementLevel nextLevel = null;
            Integer highestUnlockedLevelId = null;
            String highestUnlockedLevelName = null;
            LocalDateTime unlockedAt = null;
            String highestUnlockedLevelImageUrl = null;
            
            // Xác định loại điểm cần thiết cho achievement này (lấy từ level đầu tiên)
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
                            currentPoints = stats.getTotalExperience(); // Default
                        }
                    }
                } catch (Exception e) {
                    currentPoints = stats.getTotalExperience(); // Default
                }
            }
            
            dto.setCurrentPoints(currentPoints);
            
            // Tìm level cao nhất đã unlock và level tiếp theo
            // QUAN TRỌNG: Chỉ coi là unlocked nếu ĐÃ CÓ TRONG DB (isDisplayed = true)
            // Không dựa vào điểm hiện tại để xác định status, vì điểm chỉ dùng để unlock achievements mới
            for (GamificationAchievementLevel level : levels) {
                Integer levelId = level.getId();
                boolean isUnlockedInDB = userAchievementMap.containsKey(levelId);
                
                // Level được coi là unlocked CHỈ KHI ĐÃ CÓ TRONG DB (isDisplayed = true)
                // Không check điểm ở đây vì điểm chỉ dùng để unlock achievements mới trong checkAndUnlockAchievements
                boolean isUnlocked = isUnlockedInDB;
                
                if (isUnlocked && (highestUnlockedLevel == null || 
                    (level.getMinPointsRequired() != null && highestUnlockedLevel.getMinPointsRequired() != null &&
                     level.getMinPointsRequired() > highestUnlockedLevel.getMinPointsRequired()))) {
                    highestUnlockedLevel = level;
                    highestUnlockedLevelId = levelId;
                    highestUnlockedLevelName = level.getLevelName();
                    // Chỉ lấy unlockedAt từ DB nếu có trong DB
                    unlockedAt = isUnlockedInDB ? unlockedAtMap.get(levelId) : null;
                    highestUnlockedLevelImageUrl = level.getImageUrl() != null ? level.getImageUrl() : achievement.getImageUrl();
                }
                
                // Tìm level tiếp theo (level đầu tiên chưa unlock - cả trong DB và điểm)
                if (nextLevel == null && !isUnlocked) {
                    nextLevel = level;
                }
            }
            
            // Set thông tin level cao nhất đã unlock
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
            
            // Set thông tin level tiếp theo
            if (nextLevel != null) {
                dto.setNextLevelId(nextLevel.getId());
                dto.setNextLevelName(nextLevel.getLevelName());
                dto.setNextLevelMinPoints(nextLevel.getMinPointsRequired());
                dto.setNextLevelImageUrl(nextLevel.getImageUrl() != null ? nextLevel.getImageUrl() : achievement.getImageUrl());
            }
            
            // Tính max points required (level cao nhất trong achievement)
            GamificationAchievementLevel maxLevel = levels.get(levels.size() - 1);
            dto.setMaxPointsRequired(maxLevel.getMinPointsRequired());
            
            // Tính progress percentage
            if (dto.getIsUnlocked() && highestUnlockedLevel != null) {
                // Nếu đã unlock, progress = 100% cho level đó
                dto.setProgressPercentage(100.0);
            } else if (nextLevel != null && nextLevel.getMinPointsRequired() != null) {
                // Nếu chưa unlock, tính progress đến level tiếp theo
                Integer minForNext = nextLevel.getMinPointsRequired();
                Integer minForPrev = 0;
                // Tìm level trước đó (nếu có)
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
                dto.setProgressPercentage(100.0); // Đã unlock hết
            }
            
            // Kiểm tra xem đã unlock hết chưa
            boolean allUnlocked = true;
            for (GamificationAchievementLevel level : levels) {
                if (!userAchievementMap.containsKey(level.getId())) {
                    allUnlocked = false;
                    break;
                }
            }
            dto.setIsFullyUnlocked(allUnlocked);
            
            // Tính rarity (phần trăm user có achievement này - có ít nhất 1 level)
            long usersWithAchievement = achievementRepository.countDistinctUsersByAchievementId(achievement.getId());
            double rarity = (usersWithAchievement * 100.0) / totalUsers;
            dto.setRarityPercentage(rarity);
            
            // Set description (có thể lấy từ level đầu tiên hoặc achievement name)
            if (pointTypeName != null && nextLevel != null) {
                dto.setDescription(String.format("Đạt %d điểm %s", nextLevel.getMinPointsRequired(), pointTypeName));
            } else if (highestUnlockedLevel != null && pointTypeName != null) {
                dto.setDescription(String.format("Đạt %d điểm %s", highestUnlockedLevel.getMinPointsRequired(), pointTypeName));
            } else {
                dto.setDescription(achievement.getName());
            }
            
            // Tạo danh sách levels với thông tin unlock
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
                // Tiếp tục xử lý achievement tiếp theo, không throw exception
            }
        }
        
        System.out.println(String.format(
                "[GamificationService] getAllAchievementsWithUserStatus - Processed: %d achievements with levels, Result size: %d",
                processedCount, result.size()));
        
        return result;
    }
}