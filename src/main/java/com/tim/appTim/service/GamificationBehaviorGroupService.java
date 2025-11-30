package com.tim.appTim.service;

import com.tim.appTim.dto.GamificationBehaviorDTO;
import com.tim.appTim.dto.GamificationBehaviorGroupDTO;
import com.tim.appTim.entity.GamificationBehavior;
import com.tim.appTim.entity.GamificationBehaviorGroup;
import com.tim.appTim.repository.GamificationBehaviorGroupRepository;
import com.tim.appTim.repository.GamificationBehaviorRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationBehaviorGroupService {

    private final GamificationBehaviorGroupRepository groupRepository;
    private final GamificationBehaviorRepository behaviorRepository;

    public GamificationBehaviorGroupService(
            GamificationBehaviorGroupRepository groupRepository,
            GamificationBehaviorRepository behaviorRepository) {
        this.groupRepository = groupRepository;
        this.behaviorRepository = behaviorRepository;
    }

    @Transactional(readOnly = true)
    public List<GamificationBehaviorGroupDTO> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GamificationBehaviorGroupDTO getGroupById(Integer id) {
        GamificationBehaviorGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm hành vi với ID: " + id));
        return mapToDTO(group);
    }

    public GamificationBehaviorGroupDTO createGroup(GamificationBehaviorGroupDTO dto) {
        GamificationBehaviorGroup group = new GamificationBehaviorGroup();
        group.setName(dto.getName());
        
        GamificationBehaviorGroup saved = groupRepository.save(group);
        return mapToDTO(saved);
    }

    public GamificationBehaviorGroupDTO updateGroup(Integer id, GamificationBehaviorGroupDTO dto) {
        GamificationBehaviorGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm hành vi với ID: " + id));
        
        if (dto.getName() != null) {
            group.setName(dto.getName());
        }
        
        GamificationBehaviorGroup saved = groupRepository.save(group);
        return mapToDTO(saved);
    }

    public void deleteGroup(Integer id) {
        if (!groupRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy nhóm hành vi với ID: " + id);
        }

        groupRepository.deleteById(id);
    }

    private GamificationBehaviorGroupDTO mapToDTO(GamificationBehaviorGroup group) {
        GamificationBehaviorGroupDTO dto = new GamificationBehaviorGroupDTO();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setCreatedAt(group.getCreatedAt());

        List<GamificationBehavior> behaviors = behaviorRepository.findByGroupId(group.getId());
        List<GamificationBehaviorDTO> behaviorDTOs = behaviors.stream()
                .map(this::mapBehaviorToDTO)
                .collect(Collectors.toList());
        dto.setBehaviors(behaviorDTOs);
        
        return dto;
    }

    private GamificationBehaviorDTO mapBehaviorToDTO(GamificationBehavior behavior) {
        GamificationBehaviorDTO dto = new GamificationBehaviorDTO();
        dto.setId(behavior.getId());
        if (behavior.getGroup() != null) {
            dto.setGroupId(behavior.getGroup().getId());
            dto.setGroupName(behavior.getGroup().getName());
        }
        dto.setCode(behavior.getCode());
        dto.setName(behavior.getName());
        dto.setFrequencyType(behavior.getFrequencyType().name());
        dto.setMaxTimesPerFrequency(behavior.getMaxTimesPerFrequency());
        dto.setPointDiligence(behavior.getPointDiligence());
        dto.setPointCompetence(behavior.getPointCompetence());
        dto.setPointExperience(behavior.getPointExperience());
        dto.setCreatedAt(behavior.getCreatedAt());
        return dto;
    }
}

