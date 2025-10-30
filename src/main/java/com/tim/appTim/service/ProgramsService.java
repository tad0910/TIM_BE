package com.tim.appTim.service;

import com.tim.appTim.dto.ModuleDTO;
import com.tim.appTim.dto.ModuleSessionDTO;
import com.tim.appTim.dto.ProgramsDTO;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.entity.ProgramModule;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.repository.ModuleSessionRepository;
import com.tim.appTim.repository.ProgramModuleRepository;
import com.tim.appTim.repository.ProgramsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgramsService {
    private final ProgramsRepository programsRepository;
    private final ProgramModuleRepository programModuleRepository;
    private final ModuleSessionRepository moduleSessionRepository;

    public ProgramsService(ProgramsRepository programsRepository, 
                          ProgramModuleRepository programModuleRepository,
                          ModuleSessionRepository moduleSessionRepository) {
        this.programsRepository = programsRepository;
        this.programModuleRepository = programModuleRepository;
        this.moduleSessionRepository = moduleSessionRepository;
    }

    @Transactional(readOnly = true)
    public List<ProgramsDTO> getAllPrograms() {
        return programsRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgramsDTO getProgramById(Integer id) {
        Programs program = programsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với id = " + id));
        return toDTO(program);
    }

    @Transactional
    public ProgramsDTO createProgram(Programs program) {
        if (program.getName() == null || program.getName().isBlank()) {
            throw new BadRequestException("Tên chương trình không được để trống");
        }
        Programs savedProgram = programsRepository.save(program);
        return toDTO(savedProgram);
    }

    @Transactional
    public ProgramsDTO updateProgram(Integer id, Programs updatedProgram) {
        Programs existingProgram = programsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với id = " + id));

        if (updatedProgram.getName() != null && !updatedProgram.getName().isBlank()) {
            existingProgram.setName(updatedProgram.getName());
        }
        if (updatedProgram.getDescription() != null) {
            existingProgram.setDescription(updatedProgram.getDescription());
        }

        Programs savedProgram = programsRepository.save(existingProgram);
        return toDTO(savedProgram);
    }

    @Transactional
    public void deleteProgram(Integer id) {
        if (!programsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy chương trình với id = " + id);
        }
        programsRepository.deleteById(id);
    }

    @Transactional
    public ProgramsDTO addModulesToProgram(Integer programId, List<Integer> moduleIds) {
        Programs program = programsRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với id = " + programId));

        // Kiểm tra trùng lặp module trong program
        List<ProgramModule> currentList = programModuleRepository.findByProgramId(programId);
        java.util.Set<Integer> currentModuleIds = currentList.stream()
                .map(pm -> pm.getModule().getId())
                .collect(java.util.stream.Collectors.toSet());
        int maxPosition = currentList.stream().mapToInt(pm -> pm.getPosition() != null ? pm.getPosition() : 0).max().orElse(0);
        int added = 0;
        for (Integer moduleId : moduleIds) {
            if (!currentModuleIds.contains(moduleId)) {
                ProgramModule pm = new ProgramModule();
                ProgramModule.ProgramModuleId pmId = new ProgramModule.ProgramModuleId(programId, moduleId);
                pm.setId(pmId);
                pm.setProgram(program);
                com.tim.appTim.entity.Module module = new com.tim.appTim.entity.Module();
                module.setId(moduleId);
                pm.setModule(module);
                pm.setPosition(maxPosition + (++added));
                programModuleRepository.save(pm);
            }
        }
        // Trả về chương trình đã cập nhật
        return toDTO(programsRepository.findById(programId).orElseThrow());
    }

    private ProgramsDTO toDTO(Programs program) {
        ProgramsDTO dto = new ProgramsDTO();
        dto.setId(program.getId());
        dto.setName(program.getName());
        dto.setDescription(program.getDescription());
        
        // Load modules từ program_modules và map sang ModuleDTO
        // Sử dụng query với JOIN FETCH để tối ưu hiệu suất
        List<ModuleDTO> modules = programModuleRepository
                .findByProgramIdWithModule(program.getId())
                .stream()
                .map(pm -> {
                    ModuleDTO moduleDTO = new ModuleDTO();
                    if (pm.getModule() != null) {
                        moduleDTO.setId(pm.getModule().getId());
                        moduleDTO.setName(pm.getModule().getName());
                        moduleDTO.setDescription(pm.getModule().getDescription());
                        
                        // Load sessions của module
                        List<ModuleSessionDTO> sessions = moduleSessionRepository
                                .findByModuleIdOrderBySessionNumberAsc(pm.getModule().getId())
                                .stream()
                                .map(this::sessionToDTO)
                                .collect(Collectors.toList());
                        moduleDTO.setSessions(sessions);
                    }
                    moduleDTO.setPosition(pm.getPosition());
                    return moduleDTO;
                })
                .collect(Collectors.toList());
        
        dto.setModules(modules);
        return dto;
    }

    private ModuleSessionDTO sessionToDTO(ModuleSession session) {
        ModuleSessionDTO dto = new ModuleSessionDTO();
        dto.setId(session.getId());
        dto.setModuleId(session.getModuleId());
        dto.setSessionNumber(session.getSessionNumber());
        dto.setTitle(session.getTitle());
        dto.setContent(session.getContent());
        dto.setScheduledAt(session.getScheduledAt());
        dto.setEndDate(session.getEndDate());
        dto.setStatus(session.getStatus() != null ? session.getStatus().name() : null);
        return dto;
    }
}
