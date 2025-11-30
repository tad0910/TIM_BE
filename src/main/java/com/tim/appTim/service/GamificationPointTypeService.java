package com.tim.appTim.service;

import com.tim.appTim.dto.GamificationPointTypeDTO;
import com.tim.appTim.entity.GamificationPointType;
import com.tim.appTim.repository.GamificationPointTypeRepository;
import com.tim.appTim.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GamificationPointTypeService {

    private final GamificationPointTypeRepository pointTypeRepository;

    public GamificationPointTypeService(GamificationPointTypeRepository pointTypeRepository) {
        this.pointTypeRepository = pointTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<GamificationPointTypeDTO> getAllActivePointTypes() {
        return pointTypeRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GamificationPointTypeDTO> getDashboardPointTypes() {
        return pointTypeRepository.findByShowOnDashboardTrueAndIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GamificationPointTypeDTO getPointTypeById(Integer id) {
        GamificationPointType pointType = pointTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại điểm với ID: " + id));
        return mapToDTO(pointType);
    }

    public GamificationPointTypeDTO createPointType(GamificationPointTypeDTO dto) {
        GamificationPointType pointType = new GamificationPointType();
        pointType.setName(dto.getName());
        pointType.setDescription(dto.getDescription());
        pointType.setMaxPoints(dto.getMaxPoints());
        pointType.setImageUrl(dto.getImageUrl());
        pointType.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        pointType.setShowOnDashboard(dto.getShowOnDashboard() != null ? dto.getShowOnDashboard() : true);
        pointType.setCreatedBy(dto.getCreatedBy());
        
        GamificationPointType saved = pointTypeRepository.save(pointType);
        return mapToDTO(saved);
    }

    public GamificationPointTypeDTO updatePointType(Integer id, GamificationPointTypeDTO dto) {
        GamificationPointType pointType = pointTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại điểm với ID: " + id));
        
        if (dto.getName() != null) pointType.setName(dto.getName());
        if (dto.getDescription() != null) pointType.setDescription(dto.getDescription());
        if (dto.getMaxPoints() != null) pointType.setMaxPoints(dto.getMaxPoints());
        if (dto.getImageUrl() != null) pointType.setImageUrl(dto.getImageUrl());
        if (dto.getIsActive() != null) pointType.setIsActive(dto.getIsActive());
        if (dto.getShowOnDashboard() != null) pointType.setShowOnDashboard(dto.getShowOnDashboard());
        
        GamificationPointType saved = pointTypeRepository.save(pointType);
        return mapToDTO(saved);
    }

    public void deletePointType(Integer id) {
        if (!pointTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy loại điểm với ID: " + id);
        }
        pointTypeRepository.deleteById(id);
    }

    private GamificationPointTypeDTO mapToDTO(GamificationPointType pointType) {
        GamificationPointTypeDTO dto = new GamificationPointTypeDTO();
        dto.setId(pointType.getId());
        dto.setName(pointType.getName());
        dto.setDescription(pointType.getDescription());
        dto.setMaxPoints(pointType.getMaxPoints());
        dto.setImageUrl(pointType.getImageUrl());
        dto.setIsActive(pointType.getIsActive());
        dto.setShowOnDashboard(pointType.getShowOnDashboard());
        dto.setCreatedBy(pointType.getCreatedBy());
        dto.setCreatedAt(pointType.getCreatedAt());
        return dto;
    }
}

