package com.tim.appTim.controller;

import com.tim.appTim.dto.CreateModuleSessionRequest;
import com.tim.appTim.dto.ModuleSessionDTO;
import com.tim.appTim.dto.UpdateModuleSessionRequest;
import com.tim.appTim.service.ModuleSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/modules")
public class ModuleSessionController {

    private final ModuleSessionService moduleSessionService;

    public ModuleSessionController(ModuleSessionService moduleSessionService) {
        this.moduleSessionService = moduleSessionService;
    }

    /**
     * Lấy danh sách tất cả buổi học của một module
     */
    @GetMapping("/{moduleId}/sessions")
    public ResponseEntity<List<ModuleSessionDTO>> getSessionsByModule(@PathVariable Integer moduleId) {
        List<ModuleSessionDTO> sessions = moduleSessionService.getSessionsByModule(moduleId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Lấy chi tiết một buổi học
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ModuleSessionDTO> getSessionById(@PathVariable Long sessionId) {
        ModuleSessionDTO session = moduleSessionService.getSessionById(sessionId);
        return ResponseEntity.ok(session);
    }

    /**
     * Tạo buổi học mới
     */
    @PostMapping("/{moduleId}/sessions")
    @PreAuthorize("hasAuthority('module:create')")
    public ResponseEntity<ModuleSessionDTO> createSession(
            @PathVariable Integer moduleId,
            @RequestBody CreateModuleSessionRequest request) {
        ModuleSessionDTO session = moduleSessionService.createSession(moduleId, request);
        return ResponseEntity.ok(session);
    }

    /**
     * Cập nhật buổi học
     */
    @PutMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('module:update')")
    public ResponseEntity<ModuleSessionDTO> updateSession(
            @PathVariable Long sessionId,
            @RequestBody UpdateModuleSessionRequest request) {
        ModuleSessionDTO session = moduleSessionService.updateSession(sessionId, request);
        return ResponseEntity.ok(session);
    }

    /**
     * Xóa buổi học
     */
    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('module:delete')")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        moduleSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
