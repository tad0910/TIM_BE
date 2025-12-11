package com.tim.appTim.service;

import com.tim.appTim.dto.BehaviorPointTypeDTO;
import com.tim.appTim.dto.GamificationBehaviorDTO;
import com.tim.appTim.entity.BehaviorPointType;
import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.entity.GamificationPointType;
import com.tim.appTim.entity.NotificationTemplate;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
import com.tim.appTim.repository.GamificationPointTypeRepository;
import com.tim.appTim.repository.NotificationTemplateRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationBehaviorService {

    private final GamificationBehaviorRepository behaviorRepository;
    private final GamificationBehaviorGroupRepository groupRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final GamificationPointTypeRepository pointTypeRepository;

    public GamificationBehaviorService(
            GamificationBehaviorRepository behaviorRepository,
            GamificationBehaviorGroupRepository groupRepository,
            NotificationTemplateRepository notificationTemplateRepository,
            GamificationPointTypeRepository pointTypeRepository) {
        this.behaviorRepository = behaviorRepository;
        this.groupRepository = groupRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.pointTypeRepository = pointTypeRepository;
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
        validateCreateRequest(dto);

        behaviorRepository.findByName(dto.getName().trim())
                .ifPresent(b -> { throw new ConflictException("Hành vi với tên '" + dto.getName() + "' đã tồn tại"); });

        GamificationBehaviorGroup group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm hành vi với ID: " + dto.getGroupId()));

        GamificationBehavior behavior = new GamificationBehavior();
        behavior.setGroup(group);
        behavior.setName(dto.getName().trim());
        behavior.setFrequencyType(GamificationBehavior.FrequencyType.valueOf(dto.getFrequencyType().trim()));
        behavior.setMaxTimesPerFrequency(dto.getMaxTimesPerFrequency());
        // Set point values only if provided, otherwise use default (0)
        behavior.setPointDiligence(dto.getPointDiligence() != null ? dto.getPointDiligence() : 0);
        behavior.setPointCompetence(dto.getPointCompetence() != null ? dto.getPointCompetence() : 0);
        behavior.setPointExperience(dto.getPointExperience() != null ? dto.getPointExperience() : 0);

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

        // Handle behavior point types via owning collection to avoid orphan issues
        if (dto.getBehaviorPointTypes() != null && !dto.getBehaviorPointTypes().isEmpty()) {
            List<BehaviorPointType> bpts = new ArrayList<>();
            for (BehaviorPointTypeDTO bptDto : dto.getBehaviorPointTypes()) {
                if (bptDto.getPointTypeId() != null && bptDto.getPoints() != null) {
                    GamificationPointType pointType = pointTypeRepository.findById(bptDto.getPointTypeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại điểm thưởng với ID: " + bptDto.getPointTypeId()));

                    BehaviorPointType behaviorPointType = new BehaviorPointType();
                    behaviorPointType.setBehavior(behavior);
                    behaviorPointType.setPointType(pointType);
                    behaviorPointType.setPoints(bptDto.getPoints());

                    if (bptDto.getNotificationTemplateId() != null) {
                        NotificationTemplate template = notificationTemplateRepository.findById(bptDto.getNotificationTemplateId())
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + bptDto.getNotificationTemplateId()));
                        behaviorPointType.setNotificationTemplate(template);
                    }
                    bpts.add(behaviorPointType);
                }
            }
            behavior.getBehaviorPointTypes().clear();
            behavior.getBehaviorPointTypes().addAll(bpts);
        } else {
            behavior.getBehaviorPointTypes().clear();
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
        if (dto.getName() != null) {
            String newName = dto.getName().trim();
            if (newName.isEmpty()) {
                throw new BadRequestException("Tên hành vi không được để trống");
            }
            behaviorRepository.findByName(newName)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            throw new ConflictException("Hành vi với tên '" + newName + "' đã tồn tại");
                        }
                    });
            behavior.setName(newName);
        }
        if (dto.getFrequencyType() != null) {
            try {
                behavior.setFrequencyType(GamificationBehavior.FrequencyType.valueOf(dto.getFrequencyType().trim()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("frequencyType không hợp lệ");
            }
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

        // Handle behavior point types via owning collection (orphanRemoval)
        if (dto.getBehaviorPointTypes() != null) {
            behavior.getBehaviorPointTypes().clear();
            for (BehaviorPointTypeDTO bptDto : dto.getBehaviorPointTypes()) {
                if (bptDto.getPointTypeId() != null && bptDto.getPoints() != null) {
                    GamificationPointType pointType = pointTypeRepository.findById(bptDto.getPointTypeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại điểm thưởng với ID: " + bptDto.getPointTypeId()));

                    BehaviorPointType behaviorPointType = new BehaviorPointType();
                    behaviorPointType.setBehavior(behavior);
                    behaviorPointType.setPointType(pointType);
                    behaviorPointType.setPoints(bptDto.getPoints());

                    if (bptDto.getNotificationTemplateId() != null) {
                        NotificationTemplate template = notificationTemplateRepository.findById(bptDto.getNotificationTemplateId())
                                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template với ID: " + bptDto.getNotificationTemplateId()));
                        behaviorPointType.setNotificationTemplate(template);
                    }

                    behavior.getBehaviorPointTypes().add(behaviorPointType);
                }
            }
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
        
        // Map behavior point types
        if (behavior.getBehaviorPointTypes() != null && !behavior.getBehaviorPointTypes().isEmpty()) {
            List<BehaviorPointTypeDTO> bptDtos = behavior.getBehaviorPointTypes().stream()
                    .map(bpt -> {
                        BehaviorPointTypeDTO bptDto = new BehaviorPointTypeDTO();
                        bptDto.setId(bpt.getId());
                        if (bpt.getPointType() != null) {
                            bptDto.setPointTypeId(bpt.getPointType().getId());
                            bptDto.setPointTypeName(bpt.getPointType().getName());
                        }
                        bptDto.setPoints(bpt.getPoints());
                        if (bpt.getNotificationTemplate() != null) {
                            bptDto.setNotificationTemplateId(bpt.getNotificationTemplate().getId());
                        }
                        return bptDto;
                    })
                    .collect(Collectors.toList());
            dto.setBehaviorPointTypes(bptDtos);
        }
        
        dto.setCreatedAt(behavior.getCreatedAt());
        return dto;
    }

    private void validateCreateRequest(GamificationBehaviorDTO dto) {
        if (dto.getGroupId() == null) {
            throw new BadRequestException("groupId là bắt buộc");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new BadRequestException("name là bắt buộc");
        }
        if (dto.getFrequencyType() == null || dto.getFrequencyType().trim().isEmpty()) {
            throw new BadRequestException("frequencyType là bắt buộc");
        }
        if (dto.getMaxTimesPerFrequency() == null) {
            throw new BadRequestException("maxTimesPerFrequency là bắt buộc");
        }
        // Validate behaviorPointTypes entries if provided
        if (dto.getBehaviorPointTypes() != null) {
            dto.getBehaviorPointTypes().forEach(bpt -> {
                if (bpt.getPointTypeId() == null) {
                    throw new BadRequestException("behaviorPointTypes.pointTypeId là bắt buộc");
                }
                if (bpt.getPoints() == null) {
                    throw new BadRequestException("behaviorPointTypes.points là bắt buộc");
                }
            });
        }
    }
}

