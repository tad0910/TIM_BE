package com.tim.appTim.service;

import com.tim.appTim.dto.GamificationBehaviorDTO;
import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.entity.NotificationTemplate;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
import com.tim.appTim.repository.NotificationTemplateRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationBehaviorService {

    private final GamificationBehaviorRepository behaviorRepository;
    private final GamificationBehaviorGroupRepository groupRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;

    public GamificationBehaviorService(
            GamificationBehaviorRepository behaviorRepository,
            GamificationBehaviorGroupRepository groupRepository,
            NotificationTemplateRepository notificationTemplateRepository) {
        this.behaviorRepository = behaviorRepository;
        this.groupRepository = groupRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<GamificationBehaviorDTO> getAllBehaviors() {
        return behaviorRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GamificationBehaviorDTO getBehaviorById(Integer id) {
        GamificationBehavior behavior = behaviorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành vi với ID: " + id));
        return mapToDTO(behavior);
    }

    @Transactional(readOnly = true)
    public GamificationBehaviorDTO getBehaviorByName(String name) {
        GamificationBehavior behavior = behaviorRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành vi với tên: " + name));
        return mapToDTO(behavior);
    }

    //@Transactional(readOnly = true)
    public GamificationBehaviorDTO createBehavior(GamificationBehaviorDTO dto) {
        GamificationBehaviorGroup group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm hành vi với ID: " + dto.getGroupId()));

        GamificationBehavior behavior = new GamificationBehavior();
        behavior.setGroup(group);
        behavior.setName(dto.getName());
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.valueOf(dto.getFrequencyType()));
        behavior.setMaxTimesPerFrequency(dto.getMaxTimesPerFrequency());
        behavior.setPointDiligence(dto.getPointDiligence());
        behavior.setPointCompetence(dto.getPointCompetence());
        behavior.setPointExperience(dto.getPointExperience());

        // Set notification templates if provided
        if (dto.getNotificationTemplateDiligenceId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateDiligenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateDiligenceId()));
            behavior.setNotificationTemplateDiligence(template);
        }
        if (dto.getNotificationTemplateCompetenceId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateCompetenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateCompetenceId()));
            behavior.setNotificationTemplateCompetence(template);
        }
        if (dto.getNotificationTemplateExperienceId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateExperienceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateExperienceId()));
            behavior.setNotificationTemplateExperience(template);
        }

        GamificationBehavior saved = behaviorRepository.save(behavior);
        return mapToDTO(saved);
    }

    public GamificationBehaviorDTO updateBehavior(Integer id, GamificationBehaviorDTO dto) {
        GamificationBehavior behavior = behaviorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hành vi với ID: " + id));

        if (dto.getGroupId() != null) {
            GamificationBehaviorGroup group = groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm hành vi với ID: " + dto.getGroupId()));
            behavior.setGroup(group);
        }
        if (dto.getName() != null) behavior.setName(dto.getName());
        if (dto.getFrequencyType() != null) {
            behavior.setFrequencyType(GamificationBehavior.FrequencyType.valueOf(dto.getFrequencyType()));
        }
        if (dto.getMaxTimesPerFrequency() != null) {
            behavior.setMaxTimesPerFrequency(dto.getMaxTimesPerFrequency());
        }
        if (dto.getPointDiligence() != null) behavior.setPointDiligence(dto.getPointDiligence());
        if (dto.getPointCompetence() != null) behavior.setPointCompetence(dto.getPointCompetence());
        if (dto.getPointExperience() != null) behavior.setPointExperience(dto.getPointExperience());

        // Update notification templates if provided
        if (dto.getNotificationTemplateDiligenceId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateDiligenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateDiligenceId()));
            behavior.setNotificationTemplateDiligence(template);
        } else if (dto.getNotificationTemplateDiligenceId() == null && behavior.getNotificationTemplateDiligence() != null) {
            // Allow clearing the template by sending null
            behavior.setNotificationTemplateDiligence(null);
        }
        if (dto.getNotificationTemplateCompetenceId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateCompetenceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateCompetenceId()));
            behavior.setNotificationTemplateCompetence(template);
        } else if (dto.getNotificationTemplateCompetenceId() == null && behavior.getNotificationTemplateCompetence() != null) {
            behavior.setNotificationTemplateCompetence(null);
        }
        if (dto.getNotificationTemplateExperienceId() != null) {
            NotificationTemplate template = notificationTemplateRepository.findById(dto.getNotificationTemplateExperienceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + dto.getNotificationTemplateExperienceId()));
            behavior.setNotificationTemplateExperience(template);
        } else if (dto.getNotificationTemplateExperienceId() == null && behavior.getNotificationTemplateExperience() != null) {
            behavior.setNotificationTemplateExperience(null);
        }

        GamificationBehavior saved = behaviorRepository.save(behavior);
        return mapToDTO(saved);
    }

    public void deleteBehavior(Integer id) {
        if (!behaviorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy hành vi với ID: " + id);
        }
        behaviorRepository.deleteById(id);
    }

    private GamificationBehaviorDTO mapToDTO(GamificationBehavior behavior) {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setId(behavior.getId());
        if (behavior.getGroup() != null) {
            dto.setGroupId(behavior.getGroup().getId());
            dto.setGroupName(behavior.getGroup().getName());
        }
        dto.setName(behavior.getName());
        dto.setFrequencyType(behavior.getFrequencyType().name());
        dto.setMaxTimesPerFrequency(behavior.getMaxTimesPerFrequency());
        dto.setPointDiligence(behavior.getPointDiligence());
        dto.setPointCompetence(behavior.getPointCompetence());
        dto.setPointExperience(behavior.getPointExperience());
        if (behavior.getNotificationTemplateDiligence() != null) {
            dto.setNotificationTemplateDiligenceId(behavior.getNotificationTemplateDiligence().getId());
        }
        if (behavior.getNotificationTemplateCompetence() != null) {
            dto.setNotificationTemplateCompetenceId(behavior.getNotificationTemplateCompetence().getId());
        }
        if (behavior.getNotificationTemplateExperience() != null) {
            dto.setNotificationTemplateExperienceId(behavior.getNotificationTemplateExperience().getId());
        }
        dto.setCreatedAt(behavior.getCreatedAt());
        return dto;
    }
}

