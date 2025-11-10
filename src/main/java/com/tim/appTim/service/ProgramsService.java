package com.tim.appTim.service;

import com.tim.appTim.dto.ModuleDTO;
import com.tim.appTim.dto.ModuleSessionDTO;
import com.tim.appTim.dto.ProgramsDTO;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.entity.ProgramModule;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.UnprocessableException;
import com.tim.appTim.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgramsService {
    private final ModuleRepository moduleRepository;
    private final ClassRepository classRepository;
    private final ProgramsRepository programsRepository;
    private final ProgramModuleRepository programModuleRepository;
    private final ModuleSessionRepository moduleSessionRepository;

    public ProgramsService(ProgramsRepository programsRepository,
                           ProgramModuleRepository programModuleRepository,
                           ModuleSessionRepository moduleSessionRepository,
                           ModuleRepository moduleRepository,
                           ClassRepository classRepository) {
        this.programsRepository = programsRepository;
        this.programModuleRepository = programModuleRepository;
        this.moduleSessionRepository = moduleSessionRepository;
        this.moduleRepository = moduleRepository;
        this.classRepository = classRepository;
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
            throw new UnprocessableException("Tên chương trình không được để trống");
        }
        Programs savedProgram = programsRepository.save(program);
        return toDTO(savedProgram);
    }

    @Transactional
    public ProgramsDTO updateProgram(Integer id, Programs updatedProgram) {
        Programs existingProgram = programsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với id = " + id));

        if (updatedProgram.getName() != null && updatedProgram.getName().isBlank()) {
            throw new UnprocessableException("Tên chương trình (name) không được để trống");
        }

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
        if (classRepository.existsByProgramId(id)) {
            throw new ConflictException("Không thể xóa chương trình này vì đang được sử dụng bởi một Lớp học.");
        }
        if (programModuleRepository.existsByProgramId(id)) {
            throw new ConflictException("Không thể xóa chương trình này vì đang có các Module liên kết.");
        }
        programsRepository.deleteById(id);
    }

    @Transactional
    public ProgramsDTO addModulesToProgram(Integer programId, List<Integer> moduleIds) {
        Programs program = programsRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với id = " + programId));

        List<ProgramModule> currentList = programModuleRepository.findByProgramId(programId);
        java.util.Set<Integer> currentModuleIds = currentList.stream()
                .map(pm -> pm.getModule().getId())
                .collect(java.util.stream.Collectors.toSet());
        int maxPosition = currentList.stream().mapToInt(pm -> pm.getPosition() != null ? pm.getPosition() : 0).max().orElse(0);
        int added = 0;
        for (Integer moduleId : moduleIds) {
            if (!currentModuleIds.contains(moduleId)) {
                com.tim.appTim.entity.Module module = moduleRepository.findById(moduleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy module với id = " + moduleId));
                ProgramModule pm = new ProgramModule();
                ProgramModule.ProgramModuleId pmId = new ProgramModule.ProgramModuleId(programId, moduleId);
                pm.setId(pmId);
                pm.setProgram(program);

                pm.setModule(module);

                pm.setPosition(maxPosition + (++added));
                programModuleRepository.save(pm);
            }
        }
        return toDTO(programsRepository.findById(programId).orElseThrow());
    }

    private ProgramsDTO toDTO(Programs program) {
        ProgramsDTO dto = new ProgramsDTO();
        dto.setId(program.getId());
        dto.setName(program.getName());
        dto.setDescription(program.getDescription());

        List<ModuleDTO> modules = programModuleRepository
                .findByProgramIdWithModule(program.getId())
                .stream()
                .map(pm -> {
                    ModuleDTO moduleDTO = new ModuleDTO();
                    if (pm.getModule() != null) {
                        moduleDTO.setId(pm.getModule().getId());
                        moduleDTO.setName(pm.getModule().getName());
                        moduleDTO.setDescription(pm.getModule().getDescription());

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
        return dto;
    }
}
