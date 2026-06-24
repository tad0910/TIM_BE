package com.tim.appTim.integration;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.TuitionRoute;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.service.StudentTuitionService;
import com.tim.appTim.service.TuitionRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TuitionRouteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TuitionRouteService tuitionRouteService;

    @MockBean
    private StudentTuitionService studentTuitionService;

    private final String BASE_URL = "/tuition-routes";

    private TuitionRouteDTO createValidDTO() {
        TuitionRouteDTO dto = new TuitionRouteDTO();
        dto.setProgramId(1);
        dto.setName("Valid Route");
        dto.setType(TuitionRoute.TuitionRouteType.FULL_TIME);
        dto.setAdmissionFee(BigDecimal.valueOf(100000));
        dto.setFirstMonthFee(BigDecimal.valueOf(500000));
        dto.setTotalListedFee(BigDecimal.valueOf(5000000));
        dto.setNumberOfInstallments(1);
        dto.setFrequency(1);
        return dto;
    }

    @Test
    @WithMockUser(authorities = "tuition:read")
    void getAllRoutes_WhenAuthorized_ShouldReturn200() throws Exception {
        TuitionRouteDTO route = createValidDTO();
        route.setId(1L);
        Page<TuitionRouteDTO> page = new PageImpl<>(List.of(route));

        doReturn(page).when(tuitionRouteService).getAllRoutes(any(Pageable.class));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].name").value("Valid Route"));
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void getAllRoutes_WhenUnauthorized_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "tuition:read")
    void getRouteById_WhenExists_ShouldReturn200() throws Exception {
        TuitionRouteDTO route = createValidDTO();
        route.setId(1L);

        doReturn(route).when(tuitionRouteService).getRouteById(1L);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(authorities = "tuition:read")
    void getRouteById_WhenNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Route not found")).when(tuitionRouteService).getRouteById(999L);

        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "tuition:create")
    void createRoute_WhenAuthorizedAndValid_ShouldReturn201() throws Exception {
        TuitionRouteDTO dto = createValidDTO();
        TuitionRouteDTO created = createValidDTO();
        created.setId(1L);

        doReturn(created).when(tuitionRouteService).createRoute(any(TuitionRouteDTO.class));

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(authorities = "tuition:create")
    void createRoute_WhenInvalidInput_ShouldReturn400() throws Exception {
        // Case 1: Missing programId
        TuitionRouteDTO dto1 = createValidDTO();
        dto1.setProgramId(null);
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isBadRequest());

        // Case 2: Empty name
        TuitionRouteDTO dto2 = createValidDTO();
        dto2.setName("");
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isBadRequest());

        // Case 3: Negative fee
        TuitionRouteDTO dto3 = createValidDTO();
        dto3.setTotalListedFee(BigDecimal.valueOf(-1));
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto3)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void createRoute_WhenUnauthorized_ShouldReturn403() throws Exception {
        TuitionRouteDTO dto = createValidDTO();
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "tuition:update")
    void updateRoute_WhenAuthorizedAndValid_ShouldReturn200() throws Exception {
        TuitionRouteDTO dto = createValidDTO();
        dto.setName("Updated Name");
        TuitionRouteDTO updated = createValidDTO();
        updated.setId(1L);
        updated.setName("Updated Name");

        doReturn(updated).when(tuitionRouteService).updateRoute(eq(1L), any(TuitionRouteDTO.class));

        mockMvc.perform(put(BASE_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(authorities = "tuition:update")
    void updateRoute_WhenNotFound_ShouldReturn404() throws Exception {
        TuitionRouteDTO dto = createValidDTO();
        doThrow(new ResourceNotFoundException("Route not found")).when(tuitionRouteService).updateRoute(eq(999L),
                any(TuitionRouteDTO.class));

        mockMvc.perform(put(BASE_URL + "/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "tuition:update")
    void updateRoute_WhenInvalidInput_ShouldReturn400() throws Exception {
        TuitionRouteDTO dto = createValidDTO();
        dto.setFrequency(0); // Invalid: min 1

        mockMvc.perform(put(BASE_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "tuition:delete")
    void deleteRoute_WhenAuthorized_ShouldReturn204() throws Exception {
        doNothing().when(tuitionRouteService).deleteRoute(1L);

        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "tuition:delete")
    void deleteRoute_WhenNotFound_ShouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Route not found")).when(tuitionRouteService).deleteRoute(999L);

        mockMvc.perform(delete(BASE_URL + "/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "tuition:create")
    void batchRegister_WhenAuthorized_ShouldReturn200() throws Exception {
        Map<String, Object> payload = Map.of("programId", 1, "routeId", 2);
        Map<String, Object> result = Map.of("success", true);

        doReturn(result).when(studentTuitionService).batchRegisterByProgram(eq(1L), eq(2L), any());

        mockMvc.perform(post(BASE_URL + "/batch-register-program")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void batchRegister_WhenUnauthorized_ShouldReturn403() throws Exception {
        Map<String, Object> payload = Map.of("programId", 1, "routeId", 2);
        mockMvc.perform(post(BASE_URL + "/batch-register-program")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }
}

