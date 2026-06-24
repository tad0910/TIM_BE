package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tim.appTim.entity.Company;
import com.tim.appTim.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private Company company;
    private CompanyRequestDTO requestDTO;
    private MultipartFile logoFile;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");
        company.setShortName("TC");
        company.setType(Company.CompanyType.OFFICIAL_PARTNER);
        company.setStatus("ACTIVE");
        company.setCreatedAt(LocalDateTime.now());

        requestDTO = new CompanyRequestDTO();
        requestDTO.setName("Test Company");
        requestDTO.setShortName("TC");
        requestDTO.setType("OFFICIAL_PARTNER");
        requestDTO.setTechnologies(Set.of("Java", "Spring"));
        requestDTO.setRegions(Set.of("Hanoi", "HCMC"));
        requestDTO.setMarkets(Set.of("Vietnam"));
        requestDTO.setIntroduction("Test introduction");
        requestDTO.setWebsite("https://test.com");
        requestDTO.setPhone("0123456789");
        requestDTO.setSize("100-500");

        logoFile = mock(MultipartFile.class);
    }

    @Test
    void findAll_ShouldReturnPageOfCompanies() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Company> expectedPage = new PageImpl<>(List.of(company));
        when(companyRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<Company> result = companyService.findAll(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(company.getName(), result.getContent().get(0).getName());
        verify(companyRepository).findAll(pageable);
    }

    @Test
    void findAll_WithKeyword_ShouldReturnPageOfCompanies() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Company> expectedPage = new PageImpl<>(List.of(company));
        when(companyRepository.findAll(pageable)).thenReturn(expectedPage);

        // Act
        Page<Company> result = companyService.findAll("Test", null, pageable);

        // Assert
        assertNotNull(result);
        verify(companyRepository).findAll(pageable);
    }

    @Test
    void findById_WhenExists_ShouldReturnCompany() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        // Act
        Company result = companyService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(company.getId(), result.getId());
        assertEquals(company.getName(), result.getName());
        verify(companyRepository).findById(1L);
    }

    @Test
    void findById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            companyService.findById(999L);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy công ty"));
        verify(companyRepository).findById(999L);
    }

    @Test
    void create_WhenNameNotExists_ShouldCreateCompany() throws IOException {
        // Arrange
        when(companyRepository.existsByName(requestDTO.getName())).thenReturn(false);
        when(logoFile.isEmpty()).thenReturn(false);
        when(logoFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/logo.jpg");
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(uploadResult);
        
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Company result = companyService.create(requestDTO, logoFile);

        // Assert
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertEquals("https://cloudinary.com/logo.jpg", result.getLogoUrl());
        verify(companyRepository).existsByName(requestDTO.getName());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void create_WhenNameExists_ShouldThrowException() {
        // Arrange
        when(companyRepository.existsByName(requestDTO.getName())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            companyService.create(requestDTO, null);
        });
        assertTrue(exception.getMessage().contains("Tên doanh nghiệp đã tồn tại"));
        verify(companyRepository).existsByName(requestDTO.getName());
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void create_WhenLogoIsNull_ShouldUsePlaceholder() {
        // Arrange
        when(companyRepository.existsByName(requestDTO.getName())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Company result = companyService.create(requestDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals("https://via.placeholder.com/150", result.getLogoUrl());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void create_WhenLogoIsEmpty_ShouldUsePlaceholder() {
        // Arrange
        when(companyRepository.existsByName(requestDTO.getName())).thenReturn(false);
        when(logoFile.isEmpty()).thenReturn(true);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Company result = companyService.create(requestDTO, logoFile);

        // Assert
        assertNotNull(result);
        assertEquals("https://via.placeholder.com/150", result.getLogoUrl());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void create_WhenUploadFails_ShouldThrowException() throws IOException {
        // Arrange
        when(companyRepository.existsByName(requestDTO.getName())).thenReturn(false);
        when(logoFile.isEmpty()).thenReturn(false);
        when(logoFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenThrow(new IOException("Upload failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            companyService.create(requestDTO, logoFile);
        });
        assertTrue(exception.getMessage().contains("Lỗi upload ảnh"));
    }

    @Test
    void create_WhenTypeIsNull_ShouldCreateCompany() {
        // Arrange
        requestDTO.setType(null);
        when(companyRepository.existsByName(requestDTO.getName())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Company result = companyService.create(requestDTO, null);

        // Assert
        assertNotNull(result);
        assertNull(result.getType());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void update_WhenExists_ShouldUpdateCompany() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        Company result = companyService.update(1L, requestDTO, null);

        // Assert
        assertNotNull(result);
        verify(companyRepository).findById(1L);
        verify(companyRepository).save(company);
    }

    @Test
    void update_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            companyService.update(999L, requestDTO, null);
        });
        assertTrue(exception.getMessage().contains("Không tìm thấy công ty"));
    }

    @Test
    void update_WithLogo_ShouldUpdateLogo() throws IOException {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(logoFile.isEmpty()).thenReturn(false);
        when(logoFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://cloudinary.com/new-logo.jpg");
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(uploadResult);
        
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        Company result = companyService.update(1L, requestDTO, logoFile);

        // Assert
        assertNotNull(result);
        verify(cloudinary).uploader();
        verify(uploader).upload(any(byte[].class), any(Map.class));
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void update_WithEmptyLogo_ShouldNotUpdateLogo() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(logoFile.isEmpty()).thenReturn(true);
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        Company result = companyService.update(1L, requestDTO, logoFile);

        // Assert
        assertNotNull(result);
        verify(cloudinary, never()).uploader();
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    void delete_WhenExists_ShouldDeleteCompany() {
        // Arrange
        when(companyRepository.existsById(1L)).thenReturn(true);
        doNothing().when(companyRepository).deleteById(1L);

        // Act
        companyService.delete(1L);

        // Assert
        verify(companyRepository).existsById(1L);
        verify(companyRepository).deleteById(1L);
    }

    @Test
    void delete_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(companyRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            companyService.delete(999L);
        });
        assertTrue(exception.getMessage().contains("Công ty không tồn tại"));
        verify(companyRepository).existsById(999L);
        verify(companyRepository, never()).deleteById(anyLong());
    }

    @Test
    void create_ShouldMapAllFieldsCorrectly() {
        // Arrange
        requestDTO.setProducts(Set.of("Product1", "Product2"));
        requestDTO.setAddress("123 Test Street");
        requestDTO.setProfileUrl("https://profile.com");
        requestDTO.setBenefits("Great benefits");
        requestDTO.setFoundingDate(LocalDate.of(2020, 1, 1));
        
        when(companyRepository.existsByName(requestDTO.getName())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Company result = companyService.create(requestDTO, null);

        // Assert
        assertNotNull(result);
        assertEquals(requestDTO.getName(), result.getName());
        assertEquals(requestDTO.getShortName(), result.getShortName());
        assertEquals(Company.CompanyType.OFFICIAL_PARTNER, result.getType());
        assertEquals(requestDTO.getTechnologies(), result.getTechnologies());
        assertEquals(requestDTO.getRegions(), result.getRegions());
        assertEquals(requestDTO.getMarkets(), result.getMarkets());
        assertEquals(requestDTO.getIntroduction(), result.getIntroduction());
        assertEquals(requestDTO.getWebsite(), result.getWebsite());
        assertEquals(requestDTO.getPhone(), result.getPhone());
        assertEquals(requestDTO.getSize(), result.getSize());
        verify(companyRepository).save(any(Company.class));
    }
}


