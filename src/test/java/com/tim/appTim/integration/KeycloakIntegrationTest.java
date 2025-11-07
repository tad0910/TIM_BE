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
    private ClientErrorException mock400Error;
    private ClientErrorException mock401Error;
    private ClientErrorException mock403Error;
    private ClientErrorException mock500Error;

    @BeforeEach
    void setUp() {
        testDTO = new UpdateUserDTO();
        testDTO.setFirstName("UpdatedName");

        // Mock 404 error
        Response mock404Response = mock(Response.class);
        when(mock404Response.getStatus()).thenReturn(404);
        when(mock404Response.readEntity(String.class)).thenReturn("User not found in KC");
        mock404Error = mock(ClientErrorException.class);
        when(mock404Error.getResponse()).thenReturn(mock404Response);

        // Mock 400 error
        Response mock400Response = mock(Response.class);
        when(mock400Response.getStatus()).thenReturn(400);
        when(mock400Response.readEntity(String.class)).thenReturn("Bad request");
        mock400Error = mock(ClientErrorException.class);
        when(mock400Error.getResponse()).thenReturn(mock400Response);

        // Mock 401 error
        Response mock401Response = mock(Response.class);
        when(mock401Response.getStatus()).thenReturn(401);
        when(mock401Response.readEntity(String.class)).thenReturn("Unauthorized");
        mock401Error = mock(ClientErrorException.class);
        when(mock401Error.getResponse()).thenReturn(mock401Response);

        // Mock 403 error
        Response mock403Response = mock(Response.class);
        when(mock403Response.getStatus()).thenReturn(403);
        when(mock403Response.readEntity(String.class)).thenReturn("Forbidden");
        mock403Error = mock(ClientErrorException.class);
        when(mock403Error.getResponse()).thenReturn(mock403Response);

        // Mock 500 error (default case)
        Response mock500Response = mock(Response.class);
        when(mock500Response.getStatus()).thenReturn(500);
        when(mock500Response.readEntity(String.class)).thenReturn("Internal server error");
        mock500Error = mock(ClientErrorException.class);
        when(mock500Error.getResponse()).thenReturn(mock500Response);
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
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void updateUser_WhenKeycloakServiceThrows400_ShouldReturn400() throws Exception {
        doThrow(mock400Error).when(keycloakSyncService)
                .updateUser(eq(OTHER_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid request to Keycloak")));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void updateUser_WhenKeycloakServiceThrows401_ShouldReturn401() throws Exception {
        doThrow(mock401Error).when(keycloakSyncService)
                .updateUser(eq(OTHER_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Unauthorized access to Keycloak")));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void updateUser_WhenKeycloakServiceThrows403_ShouldReturn403() throws Exception {
        doThrow(mock403Error).when(keycloakSyncService)
                .updateUser(eq(OTHER_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Forbidden access to Keycloak")));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void updateUser_WhenKeycloakServiceThrows500_ShouldReturn500() throws Exception {
        doThrow(mock500Error).when(keycloakSyncService)
                .updateUser(eq(OTHER_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Unexpected Keycloak error")));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void updateUser_WhenServiceThrowsGenericException_ShouldReturn500() throws Exception {
        doThrow(new RuntimeException("Database connection failed"))
                .when(keycloakSyncService).updateUser(eq(OTHER_USER_ID), any(UpdateUserDTO.class));

        mockMvc.perform(put(BASE_URL + "/" + OTHER_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Internal server error during user update")));
    }

    @Test
    @WithMockUser(username = "post_owner")
    void logoutUser_WhenUserLogsOutOther_ShouldReturn403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + OTHER_USER_ID + "/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void logoutUser_WhenAdminLogsOutOther_ShouldReturn200() throws Exception {
        doNothing().when(keycloakSyncService).logoutUserFromKeycloak(OTHER_USER_ID);

        mockMvc.perform(post(BASE_URL + "/" + OTHER_USER_ID + "/logout"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockJwt("keycloak-id-cua-user-1")
    void logoutUser_WhenUserLogsOutSelf_ShouldReturn200() throws Exception {
        doNothing().when(keycloakSyncService).logoutUserFromKeycloak(SELF_USER_ID);

        mockMvc.perform(post(BASE_URL + "/" + SELF_USER_ID + "/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã vô hiệu hóa tất cả phiên làm việc")));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void logoutUser_WhenKeycloakServiceThrows404_ShouldReturn404() throws Exception {
        doThrow(mock404Error).when(keycloakSyncService).logoutUserFromKeycloak(OTHER_USER_ID);

        mockMvc.perform(post(BASE_URL + "/" + OTHER_USER_ID + "/logout"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Không tìm thấy người dùng với ID")));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void logoutUser_WhenKeycloakServiceThrowsOtherClientError_ShouldReturnThatStatus() throws Exception {
        doThrow(mock400Error).when(keycloakSyncService).logoutUserFromKeycloak(OTHER_USER_ID);

        mockMvc.perform(post(BASE_URL + "/" + OTHER_USER_ID + "/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Lỗi từ Keycloak khi đăng xuất")));
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = {"user:update_all"})
    void logoutUser_WhenServiceThrowsGenericException_ShouldReturn500() throws Exception {
        doThrow(new RuntimeException("Network error"))
                .when(keycloakSyncService).logoutUserFromKeycloak(OTHER_USER_ID);

        mockMvc.perform(post(BASE_URL + "/" + OTHER_USER_ID + "/logout"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Lỗi hệ thống")));
    }
}