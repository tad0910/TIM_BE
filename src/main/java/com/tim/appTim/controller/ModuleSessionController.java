package com.tim.appTim.controller;

import com.tim.appTim.dto.request.CreateModuleSessionRequest;
import com.tim.appTim.dto.common.ModuleSessionDTO;
import com.tim.appTim.dto.request.UpdateModuleSessionRequest;
import com.tim.appTim.service.ModuleSessionService;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import com.tim.appTim.dto.common.ModuleDTO;

@RestController
@RequestMapping("/modules")
public class ModuleSessionController {

    private final ModuleSessionService moduleSessionService;

    public ModuleSessionController(ModuleSessionService moduleSessionService) {
        this.moduleSessionService = moduleSessionService;
    }

    @GetMapping("/{moduleId}/sessions")
    public ResponseEntity<Page<ModuleSessionDTO>> getSessionsByModulePaged(@PathVariable Integer moduleId, Pageable pageable) {
        Page<ModuleSessionDTO> sessions = moduleSessionService.getSessionsByModulePaged(moduleId, pageable);
        return ResponseEntity.ok(sessions);
    }   

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ModuleSessionDTO> getSessionById(@PathVariable Long sessionId) {
        ModuleSessionDTO session = moduleSessionService.getSessionById(sessionId);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{moduleId}/sessions")
    @PreAuthorize("hasAuthority('module:create')")
    public ResponseEntity<ModuleSessionDTO> createSession(
            @PathVariable Integer moduleId,
            @RequestBody CreateModuleSessionRequest request) {
        ModuleSessionDTO session = moduleSessionService.createSession(moduleId, request);
        return ResponseEntity.ok(session);
    }

    @PutMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('module:update')")
    public ResponseEntity<ModuleSessionDTO> updateSession(
            @PathVariable Long sessionId,
            @RequestBody UpdateModuleSessionRequest request) {
        ModuleSessionDTO session = moduleSessionService.updateSession(sessionId, request);
        return ResponseEntity.ok(session);
    }

    @PutMapping("/{moduleId}/sessions")
    @PreAuthorize("hasAuthority('module:update')")
    public ResponseEntity<ModuleDTO> addSessionsToModule(@PathVariable Integer moduleId, @RequestBody List<Long> sessionIds) {
        ModuleDTO updated = moduleSessionService.addSessionsToModule(moduleId, sessionIds);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('module:delete')")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable Long sessionId) {
        moduleSessionService.deleteSession(sessionId);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", "Delete session successfully");
        body.put("sessionId", sessionId);
        return ResponseEntity.ok().body(body);
    }
}

