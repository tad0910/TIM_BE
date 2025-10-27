package com.tim.appTim.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.config.TestSecurityConfig;
import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.dto.UserImageDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.UserService;
import com.tim.appTim.util.JwtUtil;
import com.tim.appTim.service.KeycloakIntrospectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestSecurityConfig.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // Mock beans cần thiết cho security filter chain
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private KeycloakIntrospectionService keycloakIntrospectionService;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;
    private ProfileResponse profileResponse;
    private UserImageDTO userImageDTO1;
    private UserImageDTO userImageDTO2;

    @BeforeEach
    void setUp() {
        // --- Setup User ---
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        // Mock userService để trả về user này khi cần (ví dụ: trong logic isSelf nếu test bằng @SpringBootTest)
        given(userService.findByUsernameOrEmail(anyString())).willReturn(mockUser);

        // --- Setup ProfileResponse ---
        profileResponse = new ProfileResponse(); // Dùng hàm tạo rỗng
        profileResponse.setUserId(1L); // <--- SỬA THÀNH setUserId()
        profileResponse.setUsername("testuser");
        profileResponse.setEmail("test@example.com");

        // --- Setup UserImageDTO ---
        userImageDTO1 = new UserImageDTO(
                10L,                          // id
                "/uploads/image1.jpg",       // imageUrl
                "Mô tả 1",                   // description
                LocalDateTime.now()          // createdAt
        );
        userImageDTO2 = new UserImageDTO(
                11L,                          // id
                "/uploads/image2.png",       // imageUrl
                "Mô tả 2",                   // description
                LocalDateTime.now()          // createdAt
        );
    }

    // ==========================================================
    // == Tests cho Profile API (/profile) ==
    // ==========================================================

    // ----------------------------------------------------------
    // 1️⃣ GET /profile/{userId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser // Endpoint này không cần quyền cụ thể
    void getProfile_WhenUserExists_ShouldReturn200AndProfile() throws Exception {
        given(userService.getUserProfile(1L)).willReturn(profileResponse);

        mockMvc.perform(get("/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser
    void getProfile_WhenUserNotFound_ShouldReturnError() throws Exception {
        // Giả lập service ném lỗi khi không tìm thấy profile
        given(userService.getUserProfile(99L)).willThrow(new RuntimeException("Profile not found"));

        // Mong đợi 500 Internal Server Error vì RestExceptionHandler sẽ bắt lỗi RuntimeException
        mockMvc.perform(get("/profile/99"))
                .andExpect(status().isInternalServerError());
        // Hoặc mong đợi 404 nếu RestExceptionHandler xử lý RuntimeException thành 404
        // .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------
    // 2️⃣ PUT /profile/{id} (Tương tự UserController)
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:update_all"}) // Test với quyền admin
    void updateProfile_WhenAdmin_ShouldReturn200() throws Exception {
        User updatedUserData = new User();
        updatedUserData.setEmail("updated@example.com");

        User returnedUser = new User(); // User trả về từ service
        returnedUser.setId(1L);
        returnedUser.setEmail("updated@example.com");

        given(userService.update(eq(1L), any(User.class))).willReturn(returnedUser);

        mockMvc.perform(put("/profile/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUserData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }




    // ----------------------------------------------------------
    // 3️⃣ GET /profile/{userId}/images
    // ----------------------------------------------------------
    @Test
    @WithMockUser // Không cần quyền
    void getUserImages_WhenImagesExist_ShouldReturn200AndImageList() throws Exception {
        List<UserImageDTO> images = Arrays.asList(userImageDTO1, userImageDTO2);
        given(userService.getUserImages(1L)).willReturn(images);

        mockMvc.perform(get("/profile/1/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[1].id").value(11L));
    }

    @Test
    @WithMockUser
    void getUserImages_WhenNoImages_ShouldReturn200AndEmptyList() throws Exception {
        given(userService.getUserImages(1L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/profile/1/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ----------------------------------------------------------
    // 4️⃣ POST /profile/{userId}/images
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"user:update_all"}) // Giả lập admin
    void createUserImage_WhenAdminAndValid_ShouldReturn201AndImage() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imageUrl", "/uploads/new_image.jpg");
        requestBody.put("description", "Ảnh mới");

        // Giả lập DTO trả về từ service (Sửa lại chỉ dùng 4 tham số)
        UserImageDTO createdImage = new UserImageDTO(
                12L,                         // id
                "/uploads/new_image.jpg",    // imageUrl
                "Ảnh mới",                  // description
                LocalDateTime.now()         // createdAt
        );
        // Mock service trả về DTO đã sửa
        given(userService.createUserImage(1L, "/uploads/new_image.jpg", "Ảnh mới")).willReturn(createdImage);

        mockMvc.perform(post("/profile/1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated()) // Mong đợi 201 Created
                .andExpect(jsonPath("$.id").value(12L))
                .andExpect(jsonPath("$.imageUrl").value("/uploads/new_image.jpg"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"user:update_all"})
    void createUserImage_WhenImageUrlIsEmpty_ShouldReturn400() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imageUrl", ""); // imageUrl rỗng
        requestBody.put("description", "Ảnh mới");

        mockMvc.perform(post("/profile/1/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest()); // Mong đợi 400 Bad Request
    }

    // ----------------------------------------------------------
    // 5️⃣ PUT /profile/{userId}/images/{imageId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"user:update_all"})
    void updateUserImage_WhenAdminAndValid_ShouldReturn200AndUpdatedImage() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imageUrl", "/uploads/updated_image.jpg");
        requestBody.put("description", "Mô tả đã cập nhật");

        // Giả lập DTO trả về từ service (Sửa lại chỉ dùng 4 tham số)
        UserImageDTO updatedImage = new UserImageDTO(
                10L,                         // id
                "/uploads/updated_image.jpg", // imageUrl
                "Mô tả đã cập nhật",         // description
                LocalDateTime.now()         // createdAt
        );
        // Mock service trả về DTO đã sửa
        given(userService.updateUserImage(1L, 10L, "/uploads/updated_image.jpg", "Mô tả đã cập nhật")).willReturn(updatedImage);

        mockMvc.perform(put("/profile/1/images/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.description").value("Mô tả đã cập nhật"));
    }


    // ----------------------------------------------------------
    // 6️⃣ DELETE /profile/{userId}/images/{imageId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"user:update_all"})
    void deleteUserImage_WhenAdmin_ShouldReturn204() throws Exception {
        // Mock service không làm gì cả (void)
        doNothing().when(userService).deleteUserImage(1L, 10L);

        mockMvc.perform(delete("/profile/1/images/10"))
                .andExpect(status().isNoContent()); // Mong đợi 204 No Content
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"user:update_all"})
    void deleteUserImage_WhenServiceThrowsError_ShouldReturnError() throws Exception {
        // Giả lập service ném lỗi (ví dụ: không tìm thấy ảnh)
        doThrow(new RuntimeException("Image not found")).when(userService).deleteUserImage(1L, 99L);

        mockMvc.perform(delete("/profile/1/images/99"))
                .andExpect(status().isInternalServerError()); // Mong đợi 500 (hoặc 404 tùy RestExceptionHandler)
    }
}