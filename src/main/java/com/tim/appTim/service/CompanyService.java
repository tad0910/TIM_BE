package com.tim.appTim.service;

import com.tim.appTim.dto.request.CompanyRequestDTO;
import com.tim.appTim.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyService {

    Page<Company> findAll(String keyword, String type, Pageable pageable);

    Company findById(Long id);

    Company create(CompanyRequestDTO request, MultipartFile logo);

    Company update(Long id, CompanyRequestDTO request, MultipartFile logo);

    void delete(Long id);
}
