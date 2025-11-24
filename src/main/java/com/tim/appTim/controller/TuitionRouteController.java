package com.tim.appTim.controller;

import com.tim.appTim.dto.TuitionRouteDTO;
import com.tim.appTim.service.TuitionRouteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tuition-routes")
public class TuitionRouteController {

    private final TuitionRouteService tuitionRouteService;

    public TuitionRouteController(TuitionRouteService tuitionRouteService) {
        this.tuitionRouteService = tuitionRouteService;
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
    public ResponseEntity<TuitionRouteDTO> createRoute(@RequestBody TuitionRouteDTO dto) {
        TuitionRouteDTO created = tuitionRouteService.createRoute(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('tuition:update')")
    public ResponseEntity<TuitionRouteDTO> updateRoute(
            @PathVariable Long id,
            @RequestBody TuitionRouteDTO dto) {
        return ResponseEntity.ok(tuitionRouteService.updateRoute(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('tuition:delete')")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        tuitionRouteService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}