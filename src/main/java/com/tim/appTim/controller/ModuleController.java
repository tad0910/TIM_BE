package com.tim.appTim.controller;

import com.tim.appTim.dto.common.ModuleDTO;
import com.tim.appTim.dto.request.CreateModuleSessionRequest;
import com.tim.appTim.service.ModuleService;
import com.tim.appTim.service.ModuleSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/module")
public class ModuleController {

    private final ModuleService moduleService;
    private final ModuleSessionService moduleSessionService;

    public ModuleController(ModuleService moduleService, ModuleSessionService moduleSessionService) {
        this.moduleService = moduleService;
        this.moduleSessionService = moduleSessionService;
    }

    @GetMapping
    public ResponseEntity<Page<ModuleDTO>> getAllModules(Pageable pageable) {
        Page<ModuleDTO> modules = moduleService.getAllModules(pageable);
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleDTO> getModuleById(@PathVariable Integer id) {
        ModuleDTO module = moduleService.getModuleById(id);
        return ResponseEntity.ok(module);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ModuleDTO>> searchModules(
            @RequestParam(value = "keyword", required = false) String keyword,
            Pageable pageable) {

        Page<ModuleDTO> modules;
        if (keyword == null || keyword.trim().isEmpty()) {
            modules = moduleService.getAllModules(pageable);
        } else {
            modules = moduleService.searchModulesByName(keyword.trim(), pageable);
        }
        return ResponseEntity.ok(modules);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('module:create')")
    public ResponseEntity<ModuleDTO> createModule(@RequestBody ModuleDTO request) {
        ModuleDTO created = moduleService.createModule(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('module:update')")
    public ResponseEntity<ModuleDTO> updateModule(
            @PathVariable Integer id,
            @RequestBody ModuleDTO request) {
        ModuleDTO updated = moduleService.updateModule(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('module:delete')")
    public ResponseEntity<Map<String, Object>> deleteModule(@PathVariable Integer id) {
        moduleService.deleteModule(id);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Delete module successfully");
        body.put("moduleId", id);
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{moduleId}/session")
    @PreAuthorize("hasAuthority('module:create')")
    public ResponseEntity<ModuleDTO> addSessionToModule(
            @PathVariable Integer moduleId,
            @RequestBody CreateModuleSessionRequest request) {
        moduleSessionService.createSession(moduleId, request);
        ModuleDTO updatedModule = moduleService.getModuleById(moduleId);
        return ResponseEntity.ok(updatedModule);
    }
}

