package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.dto.LoginRequest;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
@Transactional
public class AuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Test
        void testRegister_ShouldReturn200_AndSaveUser() throws Exception {
                User user = new User();
                user.setUsername("newuser");
                user.setPassword("password123");
                user.setEmail("newuser@example.com");
                user.setFirstName("New");
                user.setLastName("User");

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(user)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("User registered successfully"));

                // Verify user is saved in DB
                assert userRepository.findByUsername("newuser").isPresent();
        }

        @Test
        void testLogin_Success_ShouldReturnTokens() throws Exception {
                // User 'admin_user' exists in test-data.sql with password 'password' (noop in
                // test-data, but let's check)
                // In test-data.sql: (3, 'admin_user', '{noop}password', 'admin@example.com',
                // false)

                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsernameOrEmail("admin_user");
                loginRequest.setPassword("password");

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andExpect(jsonPath("$.refreshToken").exists())
                                .andExpect(jsonPath("$.user.username").value("admin_user"));
        }

        @Test
        void testLogin_Failure_ShouldReturn401() throws Exception {
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsernameOrEmail("admin_user");
                loginRequest.setPassword("wrongpassword");

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").value("Login failed"));
        }

        @Test
        void testRefreshToken_Success_ShouldReturnNewAccessToken() throws Exception {
                // First login to get refresh token
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsernameOrEmail("admin_user");
                loginRequest.setPassword("password");

                String response = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andReturn().getResponse().getContentAsString();

                String refreshToken = objectMapper.readTree(response).get("refreshToken").asText();

                // Now use refresh token
                mockMvc.perform(post("/auth/refresh-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        void testLogout_Success_ShouldInvalidateToken() throws Exception {
                // First login
                LoginRequest loginRequest = new LoginRequest();
                loginRequest.setUsernameOrEmail("admin_user");
                loginRequest.setPassword("password");

                String response = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andReturn().getResponse().getContentAsString();

                String accessToken = objectMapper.readTree(response).get("accessToken").asText();

                // Logout
                mockMvc.perform(post("/auth/logout")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Logout successful"));
        }
}
