package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.controller.KeycloakController.UpdateUserDTO;
import com.tim.appTim.security.WithMockJwt;
import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;


@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class KeycloakIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    private final String BASE_URL = "/api/v1/keycloak/users";
    private final String SELF_USER_ID = "keycloak-id-cua-user-1";
    private final String OTHER_USER_ID = "keycloak-id-cua-user-2";

    private UpdateUserDTO testDTO;
    private ClientErrorException mock404Error;

    @BeforeEach
    void setUp() {
        testDTO = new UpdateUserDTO();
        testDTO.setFirstName("UpdatedName");

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(404);
        when(mockResponse.readEntity(String.class)).thenReturn("User not found in KC");

        mock404Error = mock(ClientErrorException.class);
        when(mock404Error.getResponse()).thenReturn(mockResponse);
    }

    @Test
    @WithMockJwt("keycloak-id-cua-user-1")
    void updateUser_WhenUserUpdatesSelf_ShouldReturn200() throws Exception {
        doNothing().when(keycloakSyncService).updateUser(eq(SELF_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + SELF_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(username = "post_owner")
    void updateUser_WhenUserUpdatesOther_ShouldReturn403() throws Exception {
        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void updateUser_WhenAdminUpdatesOther_ShouldReturn200() throws Exception {
        doNothing().when(keycloakSyncService).updateUser(eq(OTHER_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void updateUser_WhenKeycloakServiceThrows404_ShouldReturn404() throws Exception {
        doThrow(mock404Error).when(keycloakSyncService)
                .updateUser(eq(OTHER_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Keycloak user not found")));
    }

    @Test
    @WithMockUser(username = "post_owner")
    void logoutUser_WhenUserLogsOutOther_ShouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + OTHER_USER_ID + "/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:logout_all"})
    void logoutUser_WhenAdminLogsOutOther_ShouldReturn200() throws Exception {
        doNothing().when(keycloakSyncService).logoutUserFromKeycloak(OTHER_USER_ID);

        mockMvc.perform(post(BASE_URL + "/" + OTHER_USER_ID + "/logout"))
                .andExpect(status().isOk());
    }
}