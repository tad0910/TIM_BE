package com.tim.appTim.service;

import com.tim.appTim.dto.InstallmentConfigDTO;
import com.tim.appTim.dto.TuitionRouteDTO;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.entity.TuitionRoute;
import com.tim.appTim.entity.TuitionInstallmentConfig;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.repository.TuitionRouteRepository;
import com.tim.appTim.repository.ProgramsRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
        BeanUtils.copyProperties(dto, route, "installmentConfigs");
        Programs program = programsRepository.findById(dto.getProgramId())
                .orElseThrow(
                        () -> new RuntimeException("Chương trình học không tồn tại với ID: " + dto.getProgramId()));

        route.setProgram(program);
        if (tuitionRouteRepository.existsByProgram_Id(program.getId())) {
            throw new RuntimeException("Chương trình đã có lộ trình học phí, không thể tạo thêm");
        }

        // Validate and save installment configs if provided
        if (dto.getInstallmentConfigs() != null && !dto.getInstallmentConfigs().isEmpty()) {
            validateInstallmentConfigs(dto.getInstallmentConfigs(), dto.getTotalListedFee(),
                    dto.getNumberOfInstallments());

            for (InstallmentConfigDTO configDTO : dto.getInstallmentConfigs()) {
                TuitionInstallmentConfig config = new TuitionInstallmentConfig();
                config.setInstallmentNumber(configDTO.getInstallmentNumber());
                config.setBaseAmount(configDTO.getBaseAmount());
                config.setDaysFromPrevious(configDTO.getDaysFromPrevious());
                config.setTuitionRoute(route);
                route.getInstallmentConfigs().add(config);
            }
        }

        TuitionRoute savedRoute = tuitionRouteRepository.save(route);
        TuitionRouteDTO resultDTO = convertToDTO(savedRoute);
        resultDTO.setProgramId(program.getId());
        return resultDTO;
    }

    @Transactional
    public TuitionRouteDTO updateRoute(Long id, TuitionRouteDTO dto) {
        TuitionRoute existingRoute = tuitionRouteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lộ trình để cập nhật"));
        BeanUtils.copyProperties(dto, existingRoute, "id", "installmentConfigs", "program");

        // Update installment configs if provided
        if (dto.getInstallmentConfigs() != null) {
            validateInstallmentConfigs(dto.getInstallmentConfigs(), dto.getTotalListedFee(),
                    dto.getNumberOfInstallments());

            existingRoute.getInstallmentConfigs().clear();
            for (InstallmentConfigDTO configDTO : dto.getInstallmentConfigs()) {
                TuitionInstallmentConfig config = new TuitionInstallmentConfig();
                config.setInstallmentNumber(configDTO.getInstallmentNumber());
                config.setBaseAmount(configDTO.getBaseAmount());
                config.setDaysFromPrevious(configDTO.getDaysFromPrevious());
                config.setTuitionRoute(existingRoute);
                existingRoute.getInstallmentConfigs().add(config);
            }
        }

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
        BeanUtils.copyProperties(entity, dto, "installmentConfigs");
        if (entity.getProgram() != null) {
            dto.setProgramId(entity.getProgram().getId());
        }

        // Convert configs to DTOs
        if (entity.getInstallmentConfigs() != null && !entity.getInstallmentConfigs().isEmpty()) {
            List<InstallmentConfigDTO> configDTOs = entity.getInstallmentConfigs().stream()
                    .map(config -> {
                        InstallmentConfigDTO configDTO = new InstallmentConfigDTO();
                        configDTO.setInstallmentNumber(config.getInstallmentNumber());
                        configDTO.setBaseAmount(config.getBaseAmount());
                        configDTO.setDaysFromPrevious(config.getDaysFromPrevious());
                        return configDTO;
                    })
                    .collect(Collectors.toList());
            dto.setInstallmentConfigs(configDTOs);
        }

        return dto;
    }

    private void validateInstallmentConfigs(List<InstallmentConfigDTO> configs, BigDecimal totalFee,
            Integer expectedCount) {
        // 1. Check count matches
        if (configs.size() != expectedCount) {
            throw new BadRequestException(
                    "Số lượng cấu hình (" + configs.size() + ") không khớp với số kỳ (" + expectedCount + ")");
        }

        // 2. Check sum equals totalListedFee
        BigDecimal sum = configs.stream()
                .map(InstallmentConfigDTO::getBaseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sum.compareTo(totalFee) != 0) {
            throw new BadRequestException("Tổng tiền các kỳ (" + sum + ") phải bằng tổng học phí (" + totalFee + ")");
        }

        // 3. Check installmentNumber sequence (1, 2, 3...)
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).getInstallmentNumber() != i + 1) {
                throw new BadRequestException("Số kỳ phải liên tục từ 1 đến " + expectedCount);
            }
        }
    }
}
