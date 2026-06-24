package com.tim.appTim.controller;

import com.tim.appTim.dto.common.ProgramsDTO;
import com.tim.appTim.entity.Programs;
import com.tim.appTim.service.ProgramsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/programs")
public class ProgramsController {

    private final ProgramsService programsService;

    public ProgramsController(ProgramsService programsService) {
        this.programsService = programsService;
    }

    @GetMapping
    public ResponseEntity<Page<ProgramsDTO>> getAllPrograms(Pageable pageable) {
        return ResponseEntity.ok(programsService.getAllPrograms(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramsDTO> getProgramById(@PathVariable Integer id) {
        return ResponseEntity.ok(programsService.getProgramById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('program:create')")
    public ResponseEntity<ProgramsDTO> createProgram(@RequestBody Programs program) {
        return ResponseEntity.ok(programsService.createProgram(program));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('program:update')")
    public ResponseEntity<ProgramsDTO> updateProgram(@PathVariable Integer id, @RequestBody Programs program) {
        return ResponseEntity.ok(programsService.updateProgram(id, program));
    }

    @PutMapping("/{programId}/modules")
    @PreAuthorize("hasAuthority('program:update')")
    public ResponseEntity<ProgramsDTO> addModulesToProgram(@PathVariable Integer programId, @RequestBody List<Integer> moduleIds) {
        ProgramsDTO updated = programsService.addModulesToProgram(programId, moduleIds);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('program:delete')")
    public ResponseEntity<Void> deleteProgram(@PathVariable Integer id) {
        programsService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }
}

