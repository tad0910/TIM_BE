package com.tim.appTim.service;

import com.tim.appTim.dto.TuitionRouteDTO;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.entity.TuitionRoute;
import com.tim.appTim.repository.TuitionRouteRepository;
import com.tim.appTim.repository.ProgramsRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TuitionRouteService {

    private final TuitionRouteRepository tuitionRouteRepository;
    private final ProgramsRepository programsRepository;

    public TuitionRouteService(TuitionRouteRepository tuitionRouteRepository, ProgramsRepository programsRepository) {
        this.tuitionRouteRepository = tuitionRouteRepository;
        this.programsRepository = programsRepository;
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
        Programs program = programsRepository.findById(dto.getProgramId())
                .orElseThrow(() -> new RuntimeException("Chương trình học không tồn tại với ID: " + dto.getProgramId()));

        route.setProgram(program);
        TuitionRoute savedRoute = tuitionRouteRepository.save(route);
        TuitionRouteDTO resultDTO = convertToDTO(savedRoute);
        resultDTO.setProgramId(program.getId());
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
