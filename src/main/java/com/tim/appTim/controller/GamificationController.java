package com.tim.appTim.controller;

import com.tim.appTim.dto.*;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.GamificationAchievement;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gamification")
public class GamificationController {

    private final GamificationService gamificationService;
    private final GamificationPointTypeService pointTypeService;
    private final GamificationBehaviorService behaviorService;
    private final GamificationBehaviorGroupService behaviorGroupService;
    private final GamificationAchievementService achievementService;
    private final UserService userService;
    private final RankingService rankingService;
    private final com.tim.appTim.service.GamificationGuideService guideService;

    @Autowired
    private FileUploadService fileUploadService;

    public GamificationController(
            GamificationService gamificationService,
            GamificationPointTypeService pointTypeService,
            GamificationBehaviorService behaviorService,
            GamificationBehaviorGroupService behaviorGroupService,
            GamificationAchievementService achievementService,
            UserService userService,
            RankingService rankingService,
            com.tim.appTim.service.GamificationGuideService guideService) {
        this.gamificationService = gamificationService;
        this.pointTypeService = pointTypeService;
        this.behaviorService = behaviorService;
        this.behaviorGroupService = behaviorGroupService;
        this.achievementService = achievementService;
        this.userService = userService;
        this.rankingService = rankingService;
        this.guideService = guideService;
    }

    // ========== POINT AWARDING (Core Logic) ==========
    
    @PostMapping("/award-points")
    @PreAuthorize("hasAuthority('gamification:award_points') or hasAnyRole('ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<AwardPointsResponse> awardPoints(
            @Valid @RequestBody AwardPointsRequest request) {
        AwardPointsResponse response = gamificationService.awardPoints(
                request.getUserId(), 
                request.getBehaviorId());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thống kê điểm của user hiện tại
     */
    @GetMapping("/my-stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserGamificationStatsDTO> getMyStats(Authentication authentication) {
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserGamificationStatsDTO stats = gamificationService.getUserStats(currentUser.getId());
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy thống kê điểm của user theo ID (admin/teacher)
     */
    @GetMapping("/users/{userId}/stats")
    @PreAuthorize("hasAuthority('gamification:read_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<UserGamificationStatsDTO> getUserStats(@PathVariable Long userId) {
        UserGamificationStatsDTO stats = gamificationService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Lấy lịch sử nhận điểm của user hiện tại
     */
    @GetMapping("/my-point-logs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserPointLogDTO>> getMyPointLogs(
            Authentication authentication,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<UserPointLogDTO> logs = gamificationService.getUserPointLogs(currentUser.getId(), pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Lấy lịch sử nhận điểm của user theo ID
     */
    @GetMapping("/users/{userId}/point-logs")
    @PreAuthorize("hasAuthority('gamification:read_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<Page<UserPointLogDTO>> getUserPointLogs(
            @PathVariable Long userId,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        Page<UserPointLogDTO> logs = gamificationService.getUserPointLogs(userId, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Lấy danh sách thành tích của user hiện tại
     */
    @GetMapping("/my-achievements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserAchievementDTO>> getMyAchievements(
            Authentication authentication,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<UserAchievementDTO> achievements = gamificationService.getUserAchievements(currentUser.getId(), pageable);
        return ResponseEntity.ok(achievements);
    }

    /**
     * Lấy danh sách thành tích của user theo ID
     */
    @GetMapping("/users/{userId}/achievements")
    @PreAuthorize("hasAuthority('gamification:read_all') or @userService.isSelf(authentication, #userId)")
    public ResponseEntity<Page<UserAchievementDTO>> getUserAchievements(
            @PathVariable Long userId,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        Page<UserAchievementDTO> achievements = gamificationService.getUserAchievements(userId, pageable);
        return ResponseEntity.ok(achievements);
    }

    // ========== POINT TYPES MANAGEMENT ==========

    @GetMapping("/point-types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<GamificationPointTypeDTO>> getAllPointTypes(
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        Page<GamificationPointTypeDTO> pointTypes = pointTypeService.getAllActivePointTypes(pageable);
        return ResponseEntity.ok(pointTypes);
    }

    @GetMapping("/point-types/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GamificationPointTypeDTO>> getDashboardPointTypes() {
        List<GamificationPointTypeDTO> pointTypes = pointTypeService.getDashboardPointTypes();
        return ResponseEntity.ok(pointTypes);
    }

    @GetMapping("/point-types/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationPointTypeDTO> getPointTypeById(@PathVariable Integer id) {
        GamificationPointTypeDTO pointType = pointTypeService.getPointTypeById(id);
        return ResponseEntity.ok(pointType);
    }

    @PostMapping("/point-types")
    @PreAuthorize("hasAuthority('gamification:create')")
    public ResponseEntity<GamificationPointTypeDTO> createPointType(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer maxPoints,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean showOnDashboard,
            @RequestParam(required = false) Integer createdBy) {
        String finalImageUrl = imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = fileUploadService.uploadFile(imageFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setMaxPoints(maxPoints);
        dto.setImageUrl(finalImageUrl);
        dto.setIsActive(isActive);
        dto.setShowOnDashboard(showOnDashboard);
        dto.setCreatedBy(createdBy);
        GamificationPointTypeDTO created = pointTypeService.createPointType(dto);
        return ResponseEntity.created(URI.create("/gamification/point-types/" + created.getId()))
                .body(created);
    }

    @PutMapping("/point-types/{id}")
    @PreAuthorize("hasAuthority('gamification:update')")
    public ResponseEntity<GamificationPointTypeDTO> updatePointType(
            @PathVariable Integer id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer maxPoints,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean showOnDashboard) {
        String finalImageUrl = imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = fileUploadService.uploadFile(imageFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setMaxPoints(maxPoints);
        dto.setImageUrl(finalImageUrl);
        dto.setIsActive(isActive);
        dto.setShowOnDashboard(showOnDashboard);
        GamificationPointTypeDTO updated = pointTypeService.updatePointType(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/point-types/{id}")
    @PreAuthorize("hasAuthority('gamification:delete')")
    public ResponseEntity<Void> deletePointType(@PathVariable Integer id) {
        pointTypeService.deletePointType(id);
        return ResponseEntity.noContent().build();
    }

    // ========== BEHAVIORS MANAGEMENT ==========

    @GetMapping("/behaviors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<GamificationBehaviorDTO>> getAllBehaviors(
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        Page<GamificationBehaviorDTO> behaviors = behaviorService.getAllBehaviors(pageable);
        return ResponseEntity.ok(behaviors);
    }

    @GetMapping("/behaviors/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationBehaviorDTO> getBehaviorById(@PathVariable Integer id) {
        GamificationBehaviorDTO behavior = behaviorService.getBehaviorById(id);
        return ResponseEntity.ok(behavior);
    }

    @GetMapping("/behaviors/name/{name}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationBehaviorDTO> getBehaviorByName(@PathVariable String name) {
        GamificationBehaviorDTO behavior = behaviorService.getBehaviorByName(name);
        return ResponseEntity.ok(behavior);
    }

    @PostMapping(value = "/behaviors", consumes = "application/json")
    @PreAuthorize("hasAuthority('gamification:create')")
    public ResponseEntity<GamificationBehaviorDTO> createBehavior(
            @Valid @RequestBody GamificationBehaviorDTO dto) {
        GamificationBehaviorDTO created = behaviorService.createBehavior(dto);
        return ResponseEntity.created(URI.create("/gamification/behaviors/" + created.getId()))
                .body(created);
    }

    @PutMapping(value = "/behaviors/{id}", consumes = "application/json")
    @PreAuthorize("hasAuthority('gamification:update')")
    public ResponseEntity<GamificationBehaviorDTO> updateBehavior(
            @PathVariable Integer id,
            @Valid @RequestBody GamificationBehaviorDTO dto) {
        GamificationBehaviorDTO updated = behaviorService.updateBehavior(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/behaviors/{id}")
    @PreAuthorize("hasAuthority('gamification:delete')")
    public ResponseEntity<Void> deleteBehavior(@PathVariable Integer id) {
        behaviorService.deleteBehavior(id);
        return ResponseEntity.noContent().build();
    }

    // ========== BEHAVIOR GROUPS MANAGEMENT ==========

    @GetMapping("/behavior-groups")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<GamificationBehaviorGroupDTO>> getAllBehaviorGroups(
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        Page<GamificationBehaviorGroupDTO> groups = behaviorGroupService.getAllGroups(pageable);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/behavior-groups/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationBehaviorGroupDTO> getBehaviorGroupById(@PathVariable Integer id) {
        GamificationBehaviorGroupDTO group = behaviorGroupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    @PostMapping(value = "/behavior-groups", consumes = "application/json")
    @PreAuthorize("hasAuthority('gamification:create')")
    public ResponseEntity<GamificationBehaviorGroupDTO> createBehaviorGroup(
            @Valid @RequestBody GamificationBehaviorGroupDTO dto) {
        GamificationBehaviorGroupDTO created = behaviorGroupService.createGroup(dto);
        return ResponseEntity.created(URI.create("/gamification/behavior-groups/" + created.getId()))
                .body(created);
    }

    @PutMapping(value = "/behavior-groups/{id}", consumes = "application/json")
    @PreAuthorize("hasAuthority('gamification:update')")
    public ResponseEntity<GamificationBehaviorGroupDTO> updateBehaviorGroup(
            @PathVariable Integer id,
            @Valid @RequestBody GamificationBehaviorGroupDTO dto) {
        GamificationBehaviorGroupDTO updated = behaviorGroupService.updateGroup(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/behavior-groups/{id}")
    @PreAuthorize("hasAuthority('gamification:delete')")
    public ResponseEntity<Void> deleteBehaviorGroup(@PathVariable Integer id) {
        behaviorGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    // ========== ACHIEVEMENTS MANAGEMENT ==========

    @GetMapping("/achievements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<GamificationAchievement>> getAllAchievements(
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        Page<GamificationAchievement> achievements = achievementService.getAllAchievements(pageable);
        return ResponseEntity.ok(achievements);
    }

    @GetMapping("/achievements/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationAchievement> getAchievementById(@PathVariable Integer id) {
        GamificationAchievement achievement = achievementService.getAchievementById(id);
        return ResponseEntity.ok(achievement);
    }

    @GetMapping("/achievements/{achievementId}/levels")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AchievementLevelDTO>> getAchievementLevels(
            @PathVariable Integer achievementId,
            @PageableDefault(size = 20, page = 0) Pageable pageable) {
        Page<AchievementLevelDTO> levels = achievementService.getAchievementLevels(achievementId, pageable);
        return ResponseEntity.ok(levels);
    }

    @GetMapping("/achievement-levels/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AchievementLevelDTO> getAchievementLevelById(@PathVariable Integer id) {
        AchievementLevelDTO level = achievementService.getAchievementLevelById(id);
        return ResponseEntity.ok(level);
    }

    @PostMapping("/achievements")
    @PreAuthorize("hasAuthority('gamification:create')")
    public ResponseEntity<GamificationAchievement> createAchievement(
            @RequestParam String name,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(required = false) Integer createdBy,
            Authentication authentication) {
        String finalImageUrl = imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = fileUploadService.uploadFile(imageFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        if (createdBy == null) {
            User currentUser = userService.findByUsernameOrEmail(authentication.getName());
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            createdBy = currentUser.getId().intValue();
        }
        GamificationAchievement achievement = achievementService.createAchievement(name, finalImageUrl, createdBy);
        return ResponseEntity.created(URI.create("/gamification/achievements/" + achievement.getId()))
                .body(achievement);
    }

    @PutMapping("/achievements/{id}")
    @PreAuthorize("hasAuthority('gamification:update')")
    public ResponseEntity<GamificationAchievement> updateAchievement(
            @PathVariable Integer id,
            @RequestParam(required = false) String name,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) {
        String finalImageUrl = imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = fileUploadService.uploadFile(imageFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        GamificationAchievement updated = achievementService.updateAchievement(id, name, finalImageUrl);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/achievements/{id}")
    @PreAuthorize("hasAuthority('gamification:delete')")
    public ResponseEntity<Void> deleteAchievement(@PathVariable Integer id) {
        achievementService.deleteAchievement(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/achievement-levels")
    @PreAuthorize("hasAuthority('gamification:create')")
    public ResponseEntity<AchievementLevelDTO> createAchievementLevel(
            @RequestParam(required = false) Integer achievementId,
            @RequestParam(required = false) String levelName,
            @RequestParam(required = false) Integer requiredPointTypeId,
            @RequestParam(required = false) String requiredPointTypeEnum,
            @RequestParam(required = false) Integer minPointsRequired,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "notificationTemplateId", required = false) String notificationTemplateIdStr) {
        Long notificationTemplateId = null;
        if (notificationTemplateIdStr != null && !notificationTemplateIdStr.trim().isEmpty()) {
            try {
                notificationTemplateId = Long.parseLong(notificationTemplateIdStr);
            } catch (NumberFormatException e) {
                // Invalid format, treat as null (clear template)
                notificationTemplateId = null;
            }
        }
        String finalImageUrl = imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = fileUploadService.uploadFile(imageFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        AchievementLevelDTO dto = new AchievementLevelDTO();
        dto.setAchievementId(achievementId);
        dto.setLevelName(levelName);
        dto.setRequiredPointTypeId(requiredPointTypeId);
        dto.setRequiredPointTypeEnum(requiredPointTypeEnum);
        dto.setMinPointsRequired(minPointsRequired);
        dto.setImageUrl(finalImageUrl);
        dto.setNotificationTemplateId(notificationTemplateId);
        
        AchievementLevelDTO created = achievementService.createAchievementLevel(dto);
        return ResponseEntity.created(URI.create("/gamification/achievement-levels/" + created.getId()))
                .body(created);
    }

    @PutMapping("/achievement-levels/{id}")
    @PreAuthorize("hasAuthority('gamification:update')")
    public ResponseEntity<AchievementLevelDTO> updateAchievementLevel(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer achievementId,
            @RequestParam(required = false) String levelName,
            @RequestParam(required = false) Integer requiredPointTypeId,
            @RequestParam(required = false) String requiredPointTypeEnum,
            @RequestParam(required = false) Integer minPointsRequired,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "notificationTemplateId", required = false) String notificationTemplateIdStr) {
        Long notificationTemplateId = null;
        if (notificationTemplateIdStr != null && !notificationTemplateIdStr.trim().isEmpty()) {
            try {
                notificationTemplateId = Long.parseLong(notificationTemplateIdStr);
            } catch (NumberFormatException e) {
                // Invalid format, treat as null (clear template)
                notificationTemplateId = null;
            }
        }
        String finalImageUrl = imageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                finalImageUrl = fileUploadService.uploadFile(imageFile);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
        
        AchievementLevelDTO dto = new AchievementLevelDTO();
        dto.setAchievementId(achievementId);
        dto.setLevelName(levelName);
        dto.setRequiredPointTypeId(requiredPointTypeId);
        dto.setRequiredPointTypeEnum(requiredPointTypeEnum);
        dto.setMinPointsRequired(minPointsRequired);
        dto.setImageUrl(finalImageUrl);
        dto.setNotificationTemplateId(notificationTemplateId);
        
        AchievementLevelDTO updated = achievementService.updateAchievementLevel(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/achievement-levels/{id}")
    @PreAuthorize("hasAuthority('gamification:delete')")
    public ResponseEntity<Void> deleteAchievementLevel(@PathVariable Integer id) {
        achievementService.deleteAchievementLevel(id);
        return ResponseEntity.noContent().build();
    }

    // ========== RANKING SYSTEM ==========

    /**
     * Lấy bảng xếp hạng hiện tại (real-time)
     * GET /gamification/ranking/current
     */
    @GetMapping("/ranking/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RankingResponseDTO> getCurrentRanking(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false, defaultValue = "experience") String sortBy,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer size) {
        
        Pageable pageable = PageRequest.of(page, size);
        RankingResponseDTO response = rankingService.getCurrentRanking(classId, sortBy, pageable);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/ranking/monthly")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMonthlyRanking(
            @RequestParam(required = false) String monthYear, 
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false, defaultValue = "experience") String sortBy,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer size) {

        if (monthYear == null || monthYear.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", 
                                 "message", "Parameter 'monthYear' is required (format: 'YYYY-MM')"));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        RankingResponseDTO response = rankingService.getMonthlyRanking(monthYear, classId, sortBy, pageable);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/ranking/my-position")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserRankPositionDTO> getMyRankPosition(
            Authentication authentication,
            @RequestParam(required = false) String monthYear,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false, defaultValue = "experience") String sortBy) {
        
        User currentUser = userService.findByUsernameOrEmail(authentication.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        UserRankPositionDTO position = rankingService.getUserRankPosition(
                currentUser.getId(), monthYear, classId, sortBy);
        
        if (position == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(position);
    }

    @PostMapping("/ranking/snapshot")
    @PreAuthorize("hasAuthority('gamification:create')")
    public ResponseEntity<Void> createMonthlySnapshot(
            @RequestParam(required = false) String monthYear) {
        
        rankingService.createMonthlySnapshot(monthYear);
        return ResponseEntity.ok().build();
    }

    // ========== GAMIFICATION GUIDE ==========


    @PostMapping("/guide/upload")
    @PreAuthorize("hasAuthority('gamification:create') or hasAnyRole('ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<GamificationGuideDTO> uploadGuide(
            @RequestParam(value = "guideFile", required = false) MultipartFile guideFile,
            @RequestParam(value = "file", required = false) MultipartFile file, 
            Authentication authentication) {

        MultipartFile uploadFile = guideFile != null && !guideFile.isEmpty() ? guideFile : file;
        
        try {
            User currentUser = userService.findByUsernameOrEmail(authentication.getName());
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            if (uploadFile == null || uploadFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(null);
            }

            File guideFileEntity = guideService.uploadGuide(uploadFile, currentUser.getId());
            String originalFileName = guideService.getOriginalFileName(guideFileEntity.getFileName());
            
            GamificationGuideDTO dto = new GamificationGuideDTO(
                guideFileEntity.getId(),
                guideFileEntity.getFileUrl(),
                originalFileName,
                guideFileEntity.getFileSize(),
                guideFileEntity.getFileType() != null ? guideFileEntity.getFileType().name() : "DOCUMENT"
            );

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }


    @GetMapping("/guide")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationGuideDTO> getGuide() {
        return guideService.getActiveGuide()
                .map(guideFile -> {
                    String originalFileName = guideService.getOriginalFileName(guideFile.getFileName());
                    GamificationGuideDTO dto = new GamificationGuideDTO(
                        guideFile.getId(),
                        guideFile.getFileUrl(),
                        originalFileName,
                        guideFile.getFileSize(),
                        guideFile.getFileType() != null ? guideFile.getFileType().name() : "DOCUMENT"
                    );
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/guide/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> viewGuide() {
        return guideService.getActiveGuide()
                .map(guideFile -> {
                    // Redirect đến URL của file
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .header("Location", guideFile.getFileUrl())
                            .build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

 
    @GetMapping("/guide/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GamificationGuideDTO> getGuideById(@PathVariable Integer id) {
        return guideService.getGuideById(id)
                .map(guideFile -> {
                    String originalFileName = guideService.getOriginalFileName(guideFile.getFileName());
                    GamificationGuideDTO dto = new GamificationGuideDTO(
                        guideFile.getId(),
                        guideFile.getFileUrl(),
                        originalFileName,
                        guideFile.getFileSize(),
                        guideFile.getFileType() != null ? guideFile.getFileType().name() : "DOCUMENT"
                    );
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/guide/all")
    @PreAuthorize("hasAuthority('gamification:read_all') or hasAnyRole('ROLE_ADMIN')")
    public ResponseEntity<Page<GamificationGuideDTO>> getAllGuides(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        Page<GamificationGuideDTO> guides = guideService.getAllGuides(pageable)
                .map(guideFile -> {
                    String originalFileName = guideService.getOriginalFileName(guideFile.getFileName());
                    return new GamificationGuideDTO(
                        guideFile.getId(),
                        guideFile.getFileUrl(),
                        originalFileName,
                        guideFile.getFileSize(),
                        guideFile.getFileType() != null ? guideFile.getFileType().name() : "DOCUMENT"
                    );
                });
        return ResponseEntity.ok(guides);
    }

    @PutMapping("/guide/{id}")
    @PreAuthorize("hasAuthority('gamification:update') or hasAnyRole('ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<GamificationGuideDTO> updateGuide(
            @PathVariable Integer id,
            @RequestParam(value = "guideFile", required = false) MultipartFile guideFile,
            @RequestParam(value = "file", required = false) MultipartFile file, // Backward compatibility
            Authentication authentication) {

        MultipartFile uploadFile = guideFile != null && !guideFile.isEmpty() ? guideFile : file;
        
        try {
            User currentUser = userService.findByUsernameOrEmail(authentication.getName());
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            File guideFileEntity = guideService.updateGuide(id, uploadFile, currentUser.getId());
            String originalFileName = guideService.getOriginalFileName(guideFileEntity.getFileName());
            
            GamificationGuideDTO dto = new GamificationGuideDTO(
                guideFileEntity.getId(),
                guideFileEntity.getFileUrl(),
                originalFileName,
                guideFileEntity.getFileSize(),
                guideFileEntity.getFileType() != null ? guideFileEntity.getFileType().name() : "DOCUMENT"
            );

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @DeleteMapping("/guide/{id}")
    @PreAuthorize("hasAuthority('gamification:delete') or hasAnyRole('ROLE_ADMIN', 'ROLE_GIAO_VIEN')")
    public ResponseEntity<Map<String, String>> deleteGuide(@PathVariable Integer id) {
        try {
            guideService.deleteGuide(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa file hướng dẫn thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi xóa file hướng dẫn: " + e.getMessage()));
        }
    }

    @DeleteMapping("/guide/{id}/permanent")
    @PreAuthorize("hasAuthority('gamification:delete') and hasAnyRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteGuidePermanently(@PathVariable Integer id) {
        try {
            guideService.deleteGuidePermanently(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa vĩnh viễn file hướng dẫn thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi xóa file hướng dẫn: " + e.getMessage()));
        }
    }
}

