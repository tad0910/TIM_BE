package com.tim.appTim.service;

import com.tim.appTim.dto.ModuleDTO;
import com.tim.appTim.dto.ModuleSessionDTO;
import com.tim.appTim.entity.Module;
import com.tim.appTim.entity.ModuleSession;
import com.tim.appTim.repository.ModuleRepository;
import com.tim.appTim.repository.ModuleSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.tim.appTim.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleSessionRepository moduleSessionRepository;

    public ModuleServiceImpl(
            ModuleRepository moduleRepository,
            ModuleSessionRepository moduleSessionRepository) {
        this.moduleRepository = moduleRepository;
        this.moduleSessionRepository = moduleSessionRepository;
        
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModuleDTO> getAllModules(Pageable pageable) {
        Page<Module> modulePage = moduleRepository.findAll(pageable);
        return modulePage.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleDTO getModuleById(Integer id) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found with ID: " + id));

        return toDTO(module);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModuleDTO> searchModulesByName(String keyword, Pageable pageable) {
        Page<Module> modules = moduleRepository.searchByName(keyword, pageable);
        
        if (modules.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy module nào với từ khóa: " + keyword);
        }    
        return modules.map(this::toDTO);
    }

    @Override
    @Transactional
    public ModuleDTO createModule(ModuleDTO dto) {
        Module module = new Module();
        module.setName(dto.getName());
        module.setDescription(dto.getDescription());

        module = moduleRepository.save(module);

        if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
            List<ModuleSession> sessions = new ArrayList<>();
            for (int i = 0; i < dto.getSessions().size(); i++) {
                ModuleSessionDTO sessionDTO = dto.getSessions().get(i);
                ModuleSession session = new ModuleSession();
                session.setModuleId(module.getId());
                session.setSessionNumber(sessionDTO.getSessionNumber());
                session.setTitle(sessionDTO.getTitle());
                session.setContent(sessionDTO.getContent());
                sessions.add(session);
            }
            moduleSessionRepository.saveAll(sessions);
        }

        return toDTO(module);
    }

    @Override
    @Transactional
    public ModuleDTO updateModule(Integer id, ModuleDTO dto) {
        Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with ID: " + id));

        module.setName(dto.getName());
        module.setDescription(dto.getDescription());

        module = moduleRepository.save(module);

        return toDTO(module);
    }

    @Override
    @Transactional
    public void deleteModule(Integer id) {
        if (!moduleRepository.existsById(id)) {
            throw new RuntimeException("Module not found with ID: " + id);
        }
        moduleRepository.deleteById(id);
    }


    private ModuleDTO toDTO(Module module) {
        ModuleDTO dto = new ModuleDTO();
        dto.setId(module.getId());
        dto.setName(module.getName());
        dto.setDescription(module.getDescription());

        List<ModuleSessionDTO> sessions = moduleSessionRepository
                .findByModuleIdOrderBySessionNumberAsc(module.getId())
                .stream()
                .map(this::sessionToDTO)
                .collect(Collectors.toList());
        dto.setSessions(sessions);

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
