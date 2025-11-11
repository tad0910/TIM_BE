package com.tim.appTim.service;

import com.tim.appTim.dto.ModuleDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModuleService {
    Page<ModuleDTO> getAllModules(Pageable pageable);
    ModuleDTO getModuleById(Integer id);
    ModuleDTO createModule(ModuleDTO dto);
    ModuleDTO updateModule(Integer id, ModuleDTO dto);
    void deleteModule(Integer id);
}
