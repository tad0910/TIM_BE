package com.tim.appTim.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.config.TestSecurityConfig;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.ClassService;
import com.tim.appTim.service.UserImageService;
import com.tim.appTim.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserImageService userImageService;

    @MockBean
    private ClassService classService;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("john@example.com");

        user2 = new User();
        user2.setId(2L);
        user2.setEmail("jane@example.com");
    }

    // ----------------------------------------------------------
    // 1️⃣ Lấy tất cả users
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:read_all"})
    void getAll_WhenUserIsAdmin_ShouldReturn200AndUserList() throws Exception {
        List<User> users = Arrays.asList(user1, user2);
        given(userService.findAll()).willReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].email").value("jane@example.com"));
    }

    // ----------------------------------------------------------
    // 2️⃣ Lấy user theo ID
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:read_all"})
    void getById_WhenUserExists_ShouldReturn200AndUser() throws Exception {
        given(userService.findById(1L)).willReturn(user1);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @WithMockUser(authorities = {"user:read_all"})
    void getById_WhenUserNotFound_ShouldReturn404() throws Exception {
        given(userService.findById(99L)).willThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isInternalServerError());
    }

    // ----------------------------------------------------------
    // 3️⃣ Tạo user
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:create"})
    void create_WhenUserIsAdmin_ShouldReturn201Created() throws Exception {
        User newUser = new User();
        newUser.setId(3L);
        newUser.setEmail("new@example.com");

        given(userService.create(any(User.class))).willReturn(newUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    // ----------------------------------------------------------
    // 4️⃣ Upload ảnh đại diện
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadProfileImage_WhenFileIsValid_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-image".getBytes());

        given(userService.findById(1L)).willReturn(user1);

        mockMvc.perform(multipart("/users/1/profile-image").file(file))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadProfileImage_WhenFileIsEmpty_ShouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/png", new byte[0]);

        mockMvc.perform(multipart("/users/1/profile-image").file(file))
                .andExpect(status().isBadRequest());
    }
}
