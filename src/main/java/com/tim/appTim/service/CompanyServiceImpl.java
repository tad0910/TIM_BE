package com.tim.appTim.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tim.appTim.dto.CompanyRequestDTO;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.Company.CompanyType;
import com.tim.appTim.repository.CompanyRepository;
import com.tim.appTim.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final Cloudinary cloudinary;

    @Override
    public Page<Company> findAll(String keyword, String type, Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    @Override
    public Company findById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty với ID: " + id));
    }

    @Override
    @Transactional
    public Company create(CompanyRequestDTO request, MultipartFile logo) {
        if (companyRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên doanh nghiệp đã tồn tại!");
        }

        Company company = new Company();
        mapDtoToEntity(company, request);

        company.setCreatedAt(LocalDateTime.now());
        company.setStatus("ACTIVE");

        String logoUrl = uploadLogo(logo);
        company.setLogoUrl(logoUrl != null ? logoUrl : "https://via.placeholder.com/150");

        return companyRepository.save(company);
    }

    @Override
    @Transactional
    public Company update(Long id, CompanyRequestDTO request, MultipartFile logo) {
        Company existingCompany = findById(id);

        mapDtoToEntity(existingCompany, request);

        if (logo != null && !logo.isEmpty()) {
            String newLogoUrl = uploadLogo(logo);
            existingCompany.setLogoUrl(newLogoUrl);
        }

        return companyRepository.save(existingCompany);
    }

    @Override
    public void delete(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new RuntimeException("Công ty không tồn tại");
        }
        companyRepository.deleteById(id);
    }

    private String uploadLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            Map res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "partner_logos",
                    "resource_type", "auto"
            ));
            return (String) res.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
        }
    }

    private void mapDtoToEntity(Company company, CompanyRequestDTO request) {
        company.setName(request.getName());
        company.setShortName(request.getShortName());

        if(request.getType() != null) {
            company.setType(CompanyType.valueOf(request.getType()));
        }

        company.setTechnologies(request.getTechnologies());
        company.setMarkets(request.getMarkets());
        company.setRegions(request.getRegions());
        company.setIntroduction(request.getIntroduction());
        company.setWebsite(request.getWebsite());
        company.setPhone(request.getPhone());
        company.setSize(request.getSize());
    }
}