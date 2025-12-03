package com.tim.appTim.integration;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.CompanyRequestDTO;
import com.tim.appTim.entity.Company;
import com.tim.appTim.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class CompanyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @MockBean
    private Cloudinary cloudinary;

    @Test
    @WithMockUser(username = "admin", authorities = { "company:update" })
    void testUpdateCompany_ShouldUpdateAddressAndProducts() throws Exception {
        // Setup: Create a company
        Company company = new Company();
        company.setName("Original Name");
        company.setAddress("Old Address");
        company.setProducts(Set.of("Old Product"));
        company = companyRepository.save(company);

        // Prepare update data
        CompanyRequestDTO updateRequest = new CompanyRequestDTO();
        updateRequest.setName("Updated Name");
        updateRequest.setAddress("New Address");
        updateRequest.setProducts(Set.of("New Product 1", "New Product 2"));
        updateRequest.setType("CODEGYM_TO_PARTNER"); // Required field

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "data.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(updateRequest));

        // Mock Cloudinary
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "http://new-logo.com"));

        // Execute PUT request (via multipart)
        // Note: MockMvc multipart is POST by default, need to override method to PUT
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/companies/" + company.getId())
                .file(dataPart)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        // Verify
        Company updatedCompany = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(updatedCompany.getName()).isEqualTo("Updated Name");
        assertThat(updatedCompany.getAddress()).isEqualTo("New Address");
        assertThat(updatedCompany.getProducts()).containsExactlyInAnyOrder("New Product 1", "New Product 2");
    }
}
