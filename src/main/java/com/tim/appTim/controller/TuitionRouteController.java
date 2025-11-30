package com.tim.appTim.controller;

import com.tim.appTim.dto.TuitionRouteDTO;
import com.tim.appTim.service.StudentTuitionService;
import com.tim.appTim.service.TuitionRouteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/tuition-routes")
public class TuitionRouteController {

    private final TuitionRouteService tuitionRouteService;
    private final StudentTuitionService studentTuitionService;

    public TuitionRouteController(TuitionRouteService tuitionRouteService,
            StudentTuitionService studentTuitionService) {
        this.tuitionRouteService = tuitionRouteService;
        this.studentTuitionService = studentTuitionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tuition:read') or hasAuthority('tuition:read_all')")
    public ResponseEntity<Page<TuitionRouteDTO>> getAllRoutes(Pageable pageable) {
        Page<TuitionRouteDTO> routes = tuitionRouteService.getAllRoutes(pageable);
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tuition:read') or hasAuthority('tuition:read_all')")
    public ResponseEntity<TuitionRouteDTO> getRouteById(@PathVariable Long id) {
        return ResponseEntity.ok(tuitionRouteService.getRouteById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tuition:create')")
    public ResponseEntity<TuitionRouteDTO> createRoute(@Valid @RequestBody TuitionRouteDTO dto) {
        TuitionRouteDTO created = tuitionRouteService.createRoute(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/batch-register-program")
    @PreAuthorize("hasAuthority('tuition:create') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> batchRegister(@RequestBody Map<String, Object> payload) {
        Long programId = Long.valueOf(payload.get("programId").toString());
        Long routeId = Long.valueOf(payload.get("routeId").toString());
        LocalDate enrollmentDate = LocalDate.now(); 

        Map<String, Object> result = studentTuitionService.batchRegisterByProgram(programId, routeId, enrollmentDate);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('tuition:update')")
    public ResponseEntity<TuitionRouteDTO> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody TuitionRouteDTO dto) {
        return ResponseEntity.ok(tuitionRouteService.updateRoute(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tuition:delete')")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        tuitionRouteService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}