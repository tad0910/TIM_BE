package com.tim.appTim.integration;

import com.tim.appTim.dto.TuitionOverviewDTO;
import com.tim.appTim.dto.TuitionTransactionDTO;
import com.tim.appTim.service.TuitionTransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/test-data.sql")
public class TuitionOverviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TuitionTransactionService transactionService;

    private final String BASE_URL = "/api/tuition-overview";

    private TuitionOverviewDTO createMockOverview() {
        return TuitionOverviewDTO.builder()
                .totalPaid(BigDecimal.valueOf(1000000))
                .totalRefunded(BigDecimal.ZERO)
                .totalException(BigDecimal.ZERO)
                .totalUsed(BigDecimal.valueOf(500000))
                .currentBalance(BigDecimal.valueOf(500000))
                .build();
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getSystemOverview_WhenAdmin_ShouldReturn200() throws Exception {
        TuitionOverviewDTO overview = createMockOverview();
        doReturn(overview).when(transactionService).getAdminOverview();

        mockMvc.perform(get(BASE_URL + "/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaid").value(1000000))
                .andExpect(jsonPath("$.currentBalance").value(500000));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getSystemOverview_WhenUnauthorized_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyOverview_WhenAuthenticated_ShouldReturn200() throws Exception {
        TuitionOverviewDTO overview = createMockOverview();
        // Assuming post_owner has ID 1
        doReturn(overview).when(transactionService).getStudentOverview(1L);

        mockMvc.perform(get(BASE_URL + "/my-overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaid").value(1000000));
    }

    @Test
    void getMyOverview_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/my-overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "giaovien1", userDetailsServiceBeanName = "userService")
    void getStudentOverviewByTeacher_WhenAuthorized_ShouldReturn200() throws Exception {
        TuitionOverviewDTO overview = createMockOverview();
        doReturn(overview).when(transactionService).getStudentOverview(1L);

        mockMvc.perform(get(BASE_URL + "/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaid").value(1000000));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getStudentOverviewByTeacher_WhenUnauthorized_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/student/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getMyHistory_WhenAuthenticated_ShouldReturn200() throws Exception {
        Page<TuitionTransactionDTO> page = new PageImpl<>(List.of());
        doReturn(page).when(transactionService).getTransactionHistory(eq(1L), any(Pageable.class));

        mockMvc.perform(get(BASE_URL + "/my-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void getStudentHistory_WhenAuthorized_ShouldReturn200() throws Exception {
        Page<TuitionTransactionDTO> page = new PageImpl<>(List.of());
        doReturn(page).when(transactionService).getTransactionHistory(eq(1L), any(Pageable.class));

        mockMvc.perform(get(BASE_URL + "/student/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getStudentHistory_WhenUnauthorized_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/student/2/history"))
                .andExpect(status().isForbidden());
    }
}
