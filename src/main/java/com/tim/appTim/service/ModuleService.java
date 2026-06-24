package com.tim.appTim.service;

import com.tim.appTim.dto.common.ModuleDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModuleService {
    Page<ModuleDTO> getAllModules(Pageable pageable);
    Page<ModuleDTO> searchModulesByName(String keyword, Pageable pageable);
    ModuleDTO getModuleById(Integer id);
    ModuleDTO createModule(ModuleDTO dto);
    ModuleDTO updateModule(Integer id, ModuleDTO dto);
    void deleteModule(Integer id);
}

