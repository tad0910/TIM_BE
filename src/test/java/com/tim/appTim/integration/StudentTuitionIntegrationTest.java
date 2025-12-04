package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.FeeAdjustmentDTO;
import com.tim.appTim.entity.StudentTuition;
import com.tim.appTim.entity.TuitionRoute;
import com.tim.appTim.repository.StudentTuitionRepository;
import com.tim.appTim.repository.TuitionRouteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class StudentTuitionIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private TuitionRouteRepository tuitionRouteRepository;

        @Autowired
        private StudentTuitionRepository studentTuitionRepository;

        @Test
        @WithMockUser(username = "admin_user", authorities = "tuition:create")
        void testRegisterStudent_ShouldReturnOk() throws Exception {
                // Create a TuitionRoute first as it's needed for registration
                TuitionRoute route = new TuitionRoute();
                route.setName("Standard Route");
                route.setTotalListedFee(new BigDecimal("10000000"));
                route.setType(TuitionRoute.TuitionRouteType.FULL_TIME);
                route.setFrequency(1); // Monthly
                route.setNumberOfInstallments(1);

                com.tim.appTim.entity.Programs program = new com.tim.appTim.entity.Programs();
                program.setId(100); // From test-data.sql
                route.setProgram(program);

                route = tuitionRouteRepository.save(route);

                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("studentId", 2);
                payload.put("routeId", route.getId());
                payload.put("enrollmentDate", "2024-01-01");

                mockMvc.perform(post("/api/student-tuition/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin_user", authorities = "tuition:update")
        void testAdjustTuitionFee_ShouldReturnOk() throws Exception {
                // Setup: Create route and register student to get StudentTuition
                TuitionRoute route = new TuitionRoute();
                route.setName("Standard Route");
                route.setTotalListedFee(new BigDecimal("10000000"));
                route.setType(TuitionRoute.TuitionRouteType.FULL_TIME);
                route.setFrequency(1);
                route.setNumberOfInstallments(1);

                com.tim.appTim.entity.Programs program = new com.tim.appTim.entity.Programs();
                program.setId(100); // From test-data.sql
                route.setProgram(program);

                route = tuitionRouteRepository.save(route);

                // Use register endpoint to create StudentTuition and Schedules
                Map<String, Object> registerPayload = new java.util.HashMap<>();
                registerPayload.put("studentId", 2);
                registerPayload.put("routeId", route.getId());
                registerPayload.put("enrollmentDate", "2024-01-01");

                String response = mockMvc.perform(post("/api/student-tuition/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerPayload)))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                Long studentTuitionId = objectMapper.readTree(response).get("profileId").asLong();

                FeeAdjustmentDTO dto = new FeeAdjustmentDTO();
                dto.setStudentTuitionId(studentTuitionId);
                dto.setAmount(new BigDecimal("9000000"));
                dto.setReason("Discount");

                mockMvc.perform(put("/api/student-tuition/adjust-fee")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk());
        }
}
