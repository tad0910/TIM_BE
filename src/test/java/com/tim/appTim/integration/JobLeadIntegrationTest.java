package com.tim.appTim.integration;

import com.tim.appTim.dto.JobLeadDTO;
import com.tim.appTim.service.JobLeadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class JobLeadIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
        void testCreateLead_WhenUserIsSelf_ShouldReturn200() throws Exception {
                Long studentId = 1L; // post_owner id
                JobLeadDTO request = new JobLeadDTO();
                request.setCompanyName("New Company");
                request.setShortName("NC");
                request.setAddress("Hanoi");
                request.setWebsite("https://new.com");

                mockMvc.perform(post("/api/student/job-leads/" + studentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.companyName").value("New Company"))
                                .andExpect(jsonPath("$.status").value("Mới tạo"));
        }

        @Test
        @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
        void testCreateLead_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
                Long studentId = 1L; // post_owner id, but logged in as another_user
                JobLeadDTO request = new JobLeadDTO();
                request.setCompanyName("New Company");

                mockMvc.perform(post("/api/student/job-leads/" + studentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
        void testGetMyLeads_WhenUserIsSelf_ShouldReturn200() throws Exception {
                Long studentId = 1L;

                // Create a lead first
                JobLeadDTO request = new JobLeadDTO();
                request.setCompanyName("My Company");
                mockMvc.perform(post("/api/student/job-leads/" + studentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/api/student/job-leads/my-leads/" + studentId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].companyName").value("My Company"));
        }

        @Test
        @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
        void testDeleteLead_WhenUserIsSelf_ShouldReturn200() throws Exception {
                Long studentId = 1L;

                // Create a lead
                JobLeadDTO request = new JobLeadDTO();
                request.setCompanyName("Delete Company");
                String response = mockMvc.perform(post("/api/student/job-leads/" + studentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                JobLeadDTO created = objectMapper.readValue(response, JobLeadDTO.class);
                Long leadId = created.getId();

                // Delete
                mockMvc.perform(delete("/api/student/job-leads/" + leadId + "/student/" + studentId))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Đã xóa thành công"));
        }

        @Test
        @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
        void testCreateLead_WithInvalidStudentId_ShouldReturn403() throws Exception {
                Long invalidStudentId = 999L;
                JobLeadDTO request = new JobLeadDTO();
                request.setCompanyName("Company");

                mockMvc.perform(post("/api/student/job-leads/" + invalidStudentId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
        void testDeleteLead_WhenLeadNotFound_ShouldReturn404() throws Exception {
                Long studentId = 1L;
                Long nonExistentLeadId = 9999L;

                mockMvc.perform(delete("/api/student/job-leads/" + nonExistentLeadId + "/student/" + studentId))
                                .andExpect(status().isNotFound());
        }
}
