package com.tim.appTim.integration;

import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles; // Import này
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test") // Thêm dòng này
public class PostSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deletePost_WhenUserIsOwner_ShouldReturn200() throws Exception {
        Long postIdOwnedByUser = 10L;
        mockMvc.perform(delete("/posts/" + postIdOwnedByUser))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void deletePost_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long postIdNotOwnedByUser = 10L;
        mockMvc.perform(delete("/posts/" + postIdNotOwnedByUser))
                .andExpect(status().isForbidden());
    }
}