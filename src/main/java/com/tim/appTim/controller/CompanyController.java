package com.tim.appTim.controller;

import com.tim.appTim.dto.CompanyRequestDTO;
import com.tim.appTim.entity.Company;
import com.tim.appTim.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @PreAuthorize("hasAuthority('company:read')")
    public ResponseEntity<Page<Company>> getCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(companyService.findAll(keyword, type, pageRequest));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('company:read')")
    public ResponseEntity<Company> getCompanyDetail(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('company:create')")
    public ResponseEntity<Company> createCompany(
            @RequestPart("data") @Valid CompanyRequestDTO request,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.create(request, logo));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('company:update')")
    public ResponseEntity<Company> updateCompany(
            @PathVariable Long id,
            @RequestPart("data") @Valid CompanyRequestDTO request,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return ResponseEntity.ok(companyService.update(id, request, logo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('company:delete')")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}