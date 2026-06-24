package com.tim.appTim.service;

import com.tim.appTim.dto.common.ModuleDTO;
import com.tim.appTim.dto.common.ModuleSessionDTO;
import com.tim.appTim.dto.common.ProgramsDTO;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.entity.ProgramModule;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.entity.Class;
import com.tim.appTim.exception.ConflictException;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.UnprocessableException;
import com.tim.appTim.repository.*;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProgramsService {
    private final ModuleRepository moduleRepository;
    private final ClassRepository classRepository;
    private final ProgramsRepository programsRepository;
    private final ProgramModuleRepository programModuleRepository;
    private final ModuleSessionRepository moduleSessionRepository;
    private final ClassModuleService classModuleService;

    public ProgramsService(ProgramsRepository programsRepository,
                           ProgramModuleRepository programModuleRepository,
                           ModuleSessionRepository moduleSessionRepository,
                           ModuleRepository moduleRepository,
                           ClassRepository classRepository,
                           ClassModuleService classModuleService) {
        this.programsRepository = programsRepository;
        this.programModuleRepository = programModuleRepository;
        this.moduleSessionRepository = moduleSessionRepository;
        this.moduleRepository = moduleRepository;
        this.classRepository = classRepository;
        this.classModuleService = classModuleService;
    }

    @Transactional(readOnly = true)
    public Page<ProgramsDTO> getAllPrograms(Pageable pageable) {
        Page<Programs> programPage = programsRepository.findAll(pageable); 
        return programPage.map(this::toDTO); 
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

        List<ProgramModule> currentList = programModuleRepository.findByProgramIdOrderByPositionAsc(programId);

        LinkedHashSet<Integer> desiredModuleIds = moduleIds == null
                ? new LinkedHashSet<>()
                : moduleIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        if (desiredModuleIds.isEmpty()) {
            for (ProgramModule pm : currentList) {
                programModuleRepository.delete(pm);
            }
        } else {

            for (ProgramModule pm : currentList) {
                Integer moduleId = pm.getModule() != null ? pm.getModule().getId() : null;
                if (moduleId != null && !desiredModuleIds.contains(moduleId)) {
                    programModuleRepository.delete(pm);
                }
            }

            int position = 1;
            for (Integer moduleId : desiredModuleIds) {
                ProgramModule.ProgramModuleId pmId = new ProgramModule.ProgramModuleId(programId, moduleId);
                ProgramModule pm = programModuleRepository.findById(pmId).orElse(null);
                if (pm == null) {
                    com.tim.appTim.entity.Module module = moduleRepository.findById(moduleId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy module với id = " + moduleId));
                    pm = new ProgramModule();
                    pm.setId(pmId);
                    pm.setProgram(program);
                    pm.setModule(module);
                }
                pm.setPosition(position++);
                programModuleRepository.save(pm);
            }
        }

        List<Class> affectedClasses = classRepository.findByProgramId(programId);
        List<Integer> desiredList = new ArrayList<>(desiredModuleIds);
        for (Class clazz : affectedClasses) {
            classModuleService.syncClassModules(clazz.getId(), desiredList);
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
