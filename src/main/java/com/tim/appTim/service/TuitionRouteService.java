package com.tim.appTim.service;

import com.tim.appTim.dto.TuitionRouteDTO;
import com.tim.appTim.entity.TuitionRoute;
import com.tim.appTim.repository.TuitionRouteRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TuitionRouteService {

    private final TuitionRouteRepository tuitionRouteRepository;

    public TuitionRouteService(TuitionRouteRepository tuitionRouteRepository) {
        this.tuitionRouteRepository = tuitionRouteRepository;
    }

    public Page<TuitionRouteDTO> getAllRoutes(Pageable pageable) {
        return tuitionRouteRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    public TuitionRouteDTO getRouteById(Long id) {
        TuitionRoute route = tuitionRouteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lộ trình học phí với ID: " + id));
        return convertToDTO(route);
    }

    @Transactional
    public TuitionRouteDTO createRoute(TuitionRouteDTO dto) {
        TuitionRoute route = new TuitionRoute();
        BeanUtils.copyProperties(dto, route);
        TuitionRoute savedRoute = tuitionRouteRepository.save(route);
        return convertToDTO(savedRoute);
    }

    @Transactional
    public TuitionRouteDTO updateRoute(Long id, TuitionRouteDTO dto) {
        TuitionRoute existingRoute = tuitionRouteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lộ trình để cập nhật"));
        BeanUtils.copyProperties(dto, existingRoute, "id");
        TuitionRoute updatedRoute = tuitionRouteRepository.save(existingRoute);
        return convertToDTO(updatedRoute);
    }

    @Transactional
    public void deleteRoute(Long id) {
        if (!tuitionRouteRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy lộ trình để xóa");
        }
        tuitionRouteRepository.deleteById(id);
    }

    private TuitionRouteDTO convertToDTO(TuitionRoute entity) {
        TuitionRouteDTO dto = new TuitionRouteDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
