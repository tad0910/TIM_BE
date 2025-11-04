package com.tim.appTim.service;

import com.tim.appTim.dto.ModuleDTO;
import java.util.List;

public interface ModuleService {
    List<ModuleDTO> getAllModules();
    ModuleDTO getModuleById(Integer id);
    ModuleDTO createModule(ModuleDTO dto);
    ModuleDTO updateModule(Integer id, ModuleDTO dto);
    void deleteModule(Integer id);
    ModuleDTO assignInstructor(Integer moduleId, Long instructorId);
}
