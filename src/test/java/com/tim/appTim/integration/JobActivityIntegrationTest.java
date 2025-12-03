package com.tim.appTim.integration;

import com.tim.appTim.dto.JobActivityRequest;
import com.tim.appTim.entity.JobActivity;
import com.tim.appTim.entity.JobLead;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.JobActivityRepository;
import com.tim.appTim.repository.JobLeadRepository;
import com.tim.appTim.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import com.tim.appTim.config.TestJacksonConfig;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
@Import(TestJacksonConfig.class)
public class JobActivityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobLeadRepository jobLeadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobActivityRepository jobActivityRepository;

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testAddActivity_ShouldReturn200() throws Exception {
        // Setup: Create a JobLead
        User student = userRepository.findById(1L).orElseThrow();
        JobLead lead = new JobLead();
        lead.setStudent(student);
        lead.setCompanyName("Activity Test Company");
        lead.setStatus(JobLead.LeadStatus.NEW);
        lead.setCreatedAt(LocalDateTime.now());
        lead.setCompanyName("Test Co"); // Ensure non-null
        lead = jobLeadRepository.save(lead);

        JobActivityRequest request = new JobActivityRequest();
        request.setJobLeadId(lead.getId());
        request.setActivityType("INTERVIEWING");
        request.setContent("Interview Round 1");
        request.setHappenedAt(LocalDate.now());

        MockMultipartFile jsonPart = new MockMultipartFile("data", "", "application/json",
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/student/job-activities")
                .file(jsonPart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Interview Round 1"))
                .andExpect(jsonPath("$.activityType").value("INTERVIEWING"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testGetActivities_ShouldReturnList() throws Exception {
        // Setup: Create JobLead and Activity
        User student = userRepository.findById(1L).orElseThrow();
        JobLead lead = new JobLead();
        lead.setStudent(student);
        lead.setCompanyName("Get Activity Company");
        lead.setStatus(JobLead.LeadStatus.NEW);
        lead.setCreatedAt(LocalDateTime.now());
        lead = jobLeadRepository.save(lead);

        JobActivity activity = new JobActivity();
        activity.setJobLead(lead);
        activity.setContent("Existing Activity");
        activity.setActivityType(JobLead.LeadStatus.NEW);
        activity.setHappenedAt(LocalDate.now());
        activity.setCreatedAt(LocalDateTime.now());
        jobActivityRepository.save(activity);

        mockMvc.perform(get("/api/student/job-activities/" + lead.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Existing Activity"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdateNote_ShouldReturn200() throws Exception {
        // Setup: Create JobLead and Activity
        User student = userRepository.findById(1L).orElseThrow();
        JobLead lead = new JobLead();
        lead.setStudent(student);
        lead.setCompanyName("Update Note Company");
        lead.setStatus(JobLead.LeadStatus.NEW);
        lead.setCreatedAt(LocalDateTime.now());
        lead = jobLeadRepository.save(lead);

        JobActivity activity = new JobActivity();
        activity.setJobLead(lead);
        activity.setContent("Activity to update");
        activity.setActivityType(JobLead.LeadStatus.NEW);
        activity.setHappenedAt(LocalDate.now());
        activity.setCreatedAt(LocalDateTime.now());
        activity = jobActivityRepository.save(activity);

        Map<String, String> request = Map.of("note", "Updated Note");

        mockMvc.perform(put("/api/student/job-activities/" + activity.getId() + "/note")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("Updated Note"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testAddActivity_WhenJobLeadNotFound_ShouldReturn404() throws Exception {
        JobActivityRequest request = new JobActivityRequest();
        request.setJobLeadId(9999L);
        request.setActivityType("INTERVIEWING");
        request.setContent("Content");
        request.setHappenedAt(LocalDate.now());

        MockMultipartFile jsonPart = new MockMultipartFile("data", "", "application/json",
                objectMapper.writeValueAsBytes(request));

        mockMvc.perform(multipart("/api/student/job-activities")
                .file(jsonPart))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdateNote_WhenActivityNotFound_ShouldReturn404() throws Exception {
        Map<String, String> request = Map.of("note", "Updated Note");

        mockMvc.perform(put("/api/student/job-activities/9999/note")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
