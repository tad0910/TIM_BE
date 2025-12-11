package com.tim.appTim.service;

import com.tim.appTim.dto.AchievementLevelDTO;
import com.tim.appTim.entity.GamificationAchievement;
import com.tim.appTim.entity.GamificationAchievementLevel;
import com.tim.appTim.entity.NotificationTemplate;
import com.tim.appTim.repository.GamificationAchievementRepository;
import com.tim.appTim.repository.GamificationAchievementLevelRepository;
import com.tim.appTim.repository.NotificationTemplateRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationAchievementService {

    private final GamificationAchievementRepository achievementRepository;
    private final GamificationAchievementLevelRepository levelRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final com.tim.appTim.service.GamificationService gamificationService;

    public GamificationAchievementService(
            GamificationAchievementRepository achievementRepository,
            GamificationAchievementLevelRepository levelRepository,
            NotificationTemplateRepository notificationTemplateRepository,
            @org.springframework.context.annotation.Lazy com.tim.appTim.service.GamificationService gamificationService) {
        this.achievementRepository = achievementRepository;
        this.levelRepository = levelRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.gamificationService = gamificationService;
    }

    @Transactional(readOnly = true)
    public List<GamificationAchievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public GamificationAchievement getAchievementById(Integer id) {
        return achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành tích với ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<AchievementLevelDTO> getAchievementLevels(Integer achievementId) {
        List<GamificationAchievementLevel> levels = levelRepository.findByAchievementIdOrderByMinPointsRequiredAsc(achievementId);
        return levels.stream().map(this::mapToLevelDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AchievementLevelDTO getAchievementLevelById(Integer id) {
        GamificationAchievementLevel level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp bậc thành tích với ID: " + id));
        return mapToLevelDTO(level);
    }

    public GamificationAchievement createAchievement(String name, String imageUrl, Integer createdBy) {
        GamificationAchievement achievement = new GamificationAchievement();
        achievement.setName(name);
        achievement.setImageUrl(imageUrl);
        achievement.setCreatedBy(createdBy);
        return achievementRepository.save(achievement);
    }

    public GamificationAchievement updateAchievement(Integer id, String name, String imageUrl) {
        GamificationAchievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành tích với ID: " + id));
        
        if (name != null) {
            achievement.setName(name);
        }
        if (imageUrl != null) {
            achievement.setImageUrl(imageUrl);
        }
        
        return achievementRepository.save(achievement);
    }

    public void deleteAchievement(Integer id) {
        if (!achievementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy thành tích với ID: " + id);
        }
        achievementRepository.deleteById(id);
    }

    public AchievementLevelDTO createAchievementLevel(AchievementLevelDTO dto) {
        GamificationAchievement achievement = achievementRepository.findById(dto.getAchievementId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành tích với ID: " + dto.getAchievementId()));

        GamificationAchievementLevel level = new GamificationAchievementLevel();
        level.setAchievement(achievement);
        level.setLevelName(dto.getLevelName());
        level.setRequiredPointTypeId(dto.getRequiredPointTypeId());
        if (dto.getRequiredPointTypeEnum() != null) {
            level.setRequiredPointTypeEnum(
                    GamificationAchievementLevel.PointTypeEnum.valueOf(dto.getRequiredPointTypeEnum()));
        }
        level.setMinPointsRequired(dto.getMinPointsRequired());
        level.setImageUrl(dto.getImageUrl());

        // Set notification template if provided
        if (dto.getNotificationTemplateId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateId()));
            level.setNotificationTemplate(template);
        }

        GamificationAchievementLevel saved = levelRepository.save(level);
        AchievementLevelDTO result = mapToLevelDTO(saved);
        
        // Check achievements for all users after creating new level
        // Execute after current transaction commits to ensure level is saved
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            gamificationService.checkAchievementsForAllUsers();
                        } catch (Exception e) {
                            System.err.println("Error checking achievements after creating level: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            );
        } else {
            // If no transaction, execute directly
            try {
                gamificationService.checkAchievementsForAllUsers();
            } catch (Exception e) {
                System.err.println("Error checking achievements after creating level: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return result;
    }

    public AchievementLevelDTO updateAchievementLevel(Integer id, AchievementLevelDTO dto) {
        GamificationAchievementLevel level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp bậc thành tích với ID: " + id));

        if (dto.getAchievementId() != null) {
            GamificationAchievement achievement = achievementRepository.findById(dto.getAchievementId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành tích với ID: " + dto.getAchievementId()));
            level.setAchievement(achievement);
        }
        if (dto.getLevelName() != null) level.setLevelName(dto.getLevelName());
        if (dto.getRequiredPointTypeId() != null) level.setRequiredPointTypeId(dto.getRequiredPointTypeId());
        if (dto.getRequiredPointTypeEnum() != null) {
            level.setRequiredPointTypeEnum(
                    GamificationAchievementLevel.PointTypeEnum.valueOf(dto.getRequiredPointTypeEnum()));
        }
        if (dto.getMinPointsRequired() != null) level.setMinPointsRequired(dto.getMinPointsRequired());
        if (dto.getImageUrl() != null) level.setImageUrl(dto.getImageUrl());

        // Update notification template if provided
        if (dto.getNotificationTemplateId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateId()));
            level.setNotificationTemplate(template);
        } else if (dto.getNotificationTemplateId() == null && level.getNotificationTemplate() != null) {
            // Allow clearing the template by sending null
            level.setNotificationTemplate(null);
        }

        GamificationAchievementLevel saved = levelRepository.save(level);
        AchievementLevelDTO result = mapToLevelDTO(saved);
        
        // Check achievements for all users after updating level
        // Execute after current transaction commits to ensure level is saved
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            gamificationService.checkAchievementsForAllUsers();
                        } catch (Exception e) {
                            System.err.println("Error checking achievements after updating level: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            );
        } else {
            // If no transaction, execute directly
            try {
                gamificationService.checkAchievementsForAllUsers();
            } catch (Exception e) {
                System.err.println("Error checking achievements after updating level: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return result;
    }

    public void deleteAchievementLevel(Integer id) {
        if (!levelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy cấp bậc thành tích với ID: " + id);
        }
        levelRepository.deleteById(id);
    }

    private AchievementLevelDTO mapToLevelDTO(GamificationAchievementLevel level) {
        AchievementLevelDTO dto = new AchievementLevelDTO();
        dto.setId(level.getId());
        if (level.getAchievement() != null) {
            dto.setAchievementId(level.getAchievement().getId());
            dto.setAchievementName(level.getAchievement().getName());
        }
        dto.setLevelName(level.getLevelName());
        dto.setRequiredPointTypeId(level.getRequiredPointTypeId());
        if (level.getRequiredPointTypeEnum() != null) {
            dto.setRequiredPointTypeEnum(level.getRequiredPointTypeEnum().name());
        }
        dto.setMinPointsRequired(level.getMinPointsRequired());
        dto.setImageUrl(level.getImageUrl());
        if (level.getNotificationTemplate() != null) {
            dto.setNotificationTemplateId(level.getNotificationTemplate().getId());
        }
        dto.setCreatedAt(level.getCreatedAt());
        return dto;
    }
}

