package com.tim.appTim.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.config.TestSecurityConfig;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.Role;
import com.tim.appTim.service.ClassService;
import com.tim.appTim.service.UserImageService;
import com.tim.appTim.service.UserService;
import com.tim.appTim.util.JwtUtil;
import com.tim.appTim.service.KeycloakIntrospectionService;
import com.tim.appTim.dto.ProfileResponse;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.dto.UserClassDTO;
import com.tim.appTim.entity.Class;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import java.util.*;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
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

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private KeycloakIntrospectionService keycloakIntrospectionService;

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
    // 1️⃣ GET /users
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:read_all"})
    void getAll_ShouldReturn200AndUserList() throws Exception {
        List<User> users = Arrays.asList(user1, user2);
        given(userService.findAll()).willReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].email").value("jane@example.com"));
    }

    // ----------------------------------------------------------
    // 2️⃣ GET /users/{id}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:read_all"})
    void getById_WhenUserExists_ShouldReturn200() throws Exception {
        given(userService.findById(1L)).willReturn(user1);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @WithMockUser(authorities = {"user:read_all"})
    void getById_WhenUserServiceThrows_ShouldReturn500() throws Exception {
        given(userService.findById(99L)).willThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isInternalServerError());
    }

    // ----------------------------------------------------------
    // 3️⃣ POST /users
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:create"})
    void create_ShouldReturn201Created() throws Exception {
        User inputUser = new User();
        inputUser.setEmail("new@example.com");

        User savedUser = new User();
        savedUser.setId(3L);
        savedUser.setEmail("new@example.com");

        given(userService.create(any(User.class))).willReturn(savedUser);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    // ----------------------------------------------------------
    // 4️⃣ POST /users/{id}/profile-image
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadProfileImage_WhenFileIsValid_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-image".getBytes());

        given(userService.findById(1L)).willReturn(user1);
        given(userService.updateProfileImage(eq(1L), any(String.class))).willReturn(user1);

        mockMvc.perform(multipart("/users/1/profile-image")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ảnh đại diện đã được cập nhật thành công"));
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadProfileImage_WhenFileIsEmpty_ShouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/png", new byte[0]);

        mockMvc.perform(multipart("/users/1/profile-image")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File không được để trống"));
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadProfileImage_WhenUserNotFound_ShouldReturn404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-image".getBytes());

        given(userService.findById(99L)).willReturn(null);

        mockMvc.perform(multipart("/users/99/profile-image")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadProfileImage_WhenIOExceptionOccurs_ShouldReturn500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-image".getBytes());

        given(userService.findById(1L)).willReturn(user1);
        given(userService.updateProfileImage(eq(1L), any(String.class)))
                .willThrow(new RuntimeException("File error"));

        mockMvc.perform(multipart("/users/1/profile-image")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(authorities = {"user:read"}) // Một quyền không liên quan
    void getAll_WhenUserHasInsufficientPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden()); // Mong đợi 403 Forbidden
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void update_WhenAdminUpdates_ShouldReturn200() throws Exception {
        User updatedUser = new User();
        updatedUser.setEmail("updated@example.com");

        given(userService.update(eq(1L), any(User.class))).willReturn(updatedUser);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    @WithMockUser
    void getProfileImage_WhenUserExists_ShouldReturn200() throws Exception {
        // Giả lập user1 có ảnh đại diện
        user1.setProfileImage("/uploads/avatar-cuan-user1.png");
        given(userService.findById(1L)).willReturn(user1);

        mockMvc.perform(get("/users/1/profile-image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.profileImage").value("/uploads/avatar-cuan-user1.png"));
    }

    @Test
    @WithMockUser
    void getProfileImage_WhenUserNotFound_ShouldReturn404() throws Exception {
        // Giả lập service ném lỗi khi không tìm thấy user
        given(userService.findById(99L)).willThrow(new RuntimeException("User không tồn tại"));

        mockMvc.perform(get("/users/99/profile-image"))
                .andExpect(status().isNotFound());
        // .andExpect(jsonPath("$.error").value("User không tồn tại")); // (Tùy theo RestExceptionHandler của bạn)
    }

    // ----------------------------------------------------------
    // 🔟 PUT /users/{id}/profile-image (Cập nhật ảnh bằng URL)
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void updateProfileImageByUrl_WhenAdmin_ShouldReturn200() throws Exception {
        // Chuẩn bị request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imageUrl", "/new-url/avatar.png");

        // Giả lập user sau khi được cập nhật
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setProfileImage("/new-url/avatar.png");

        given(userService.updateProfileImage(1L, "/new-url/avatar.png")).willReturn(updatedUser);

        mockMvc.perform(put("/users/1/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ảnh đại diện đã được cập nhật thành công"))
                .andExpect(jsonPath("$.user.profileImage").value("/new-url/avatar.png"));
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void updateProfileImageByUrl_WhenImageUrlIsEmpty_ShouldReturn400() throws Exception {
        // Chuẩn bị request body rỗng
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imageUrl", "");

        mockMvc.perform(put("/users/1/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("imageUrl không được để trống"));
    }

    // ----------------------------------------------------------
    // 1️⃣1️⃣ GET /users/{id}/cover-image (Lấy URL ảnh bìa)
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    void getCoverImage_WhenUserExists_ShouldReturn200() throws Exception {
        user1.setCoverImage("/uploads/cover-cuan-user1.png");
        given(userService.findById(1L)).willReturn(user1);

        mockMvc.perform(get("/users/1/cover-image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.coverImage").value("/uploads/cover-cuan-user1.png"));
    }

    // ----------------------------------------------------------
    // 1️⃣2️⃣ PUT /users/{id}/cover-image (Cập nhật ảnh bìa bằng URL)
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void updateCoverImageByUrl_WhenAdmin_ShouldReturn200() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imageUrl", "/new-url/cover.png");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setCoverImage("/new-url/cover.png");

        given(userService.updateCoverImage(1L, "/new-url/cover.png")).willReturn(updatedUser);

        mockMvc.perform(put("/users/1/cover-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ảnh bìa đã được cập nhật thành công"));
    }

    // ----------------------------------------------------------
    // 1️⃣3️⃣ DELETE /users/{id}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:delete"})
    void delete_WhenUserHasPermission_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent()); // Mong đợi 204 No Content
    }

    @Test
    @WithMockUser // User không có quyền delete
    void delete_WhenUserLacksPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isForbidden()); // Mong đợi 403 Forbidden
    }

    // 1️⃣4️⃣ GET /users/profile/{email}
    // ----------------------------------------------------------
    @Test
    @WithMockUser // Endpoint này không yêu cầu quyền cụ thể
    void getUserProfileByEmail_ShouldReturn200AndProfile() throws Exception {
        ProfileResponse mockProfile = new ProfileResponse();
        mockProfile.setEmail("john@example.com");
        //mockProfile.setFullName("John Example");
        // ... (set các trường khác nếu có)

        given(userService.getUserProfileByEmail("john@example.com")).willReturn(mockProfile);

        mockMvc.perform(get("/users/profile/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
                //.andExpect(jsonPath("$.fullName").value("John Example"));
    }

    @Test
    @WithMockUser
    void getUserClasses_ShouldReturn200AndClassList() throws Exception {
        // --- Giả lập data ---
        Class classEntity = new Class();
        classEntity.setClassName("Lớp Java");
        classEntity.setDescription("Học Java cơ bản");

        ClassMember classMember = new ClassMember();
        classMember.setClassId(10L);
        // Sử dụng tên lớp bên ngoài để truy cập enum bên trong
        classMember.setRole(ClassMember.Role.sinh_vien); // <-- SỬA DÒNG NÀY
        classMember.setJoinDate(LocalDateTime.now());
        classMember.setClassEntity(classEntity);

        List<ClassMember> mockClassMemberList = Collections.singletonList(classMember);
        // --- Hết giả lập data ---

        given(classService.getUserClasses(1L)).willReturn(mockClassMemberList);

        mockMvc.perform(get("/users/1/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalClasses").value(1))
                .andExpect(jsonPath("$.classes[0].className").value("Lớp Java"))
                .andExpect(jsonPath("$.classes[0].role").value("sinh_vien")); // JSON trả về vẫn là chuỗi "sinh_vien"
    }

    // 1️⃣6️⃣ POST /users/{id}/cover-image
    // ----------------------------------------------------------
    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadCoverImage_WhenFileIsValid_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "fake-cover-image".getBytes());

        given(userService.findById(1L)).willReturn(user1);
        given(userService.updateCoverImage(eq(1L), any(String.class))).willReturn(user1); // Sửa thành updateCoverImage

        mockMvc.perform(multipart("/users/1/cover-image") // Sửa URL
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ảnh bìa đã được cập nhật thành công")); // Sửa message
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadCoverImage_WhenFileIsEmpty_ShouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "image/png", new byte[0]);

        mockMvc.perform(multipart("/users/1/cover-image") // Sửa URL
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File không được để trống"));
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadCoverImage_WhenUserNotFound_ShouldReturn404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "fake-cover-image".getBytes());

        given(userService.findById(99L)).willReturn(null); // Hoặc ném Exception nếu service làm vậy

        mockMvc.perform(multipart("/users/99/cover-image") // Sửa URL
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {"user:update_all"})
    void uploadCoverImage_WhenIOExceptionOccurs_ShouldReturn500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "fake-cover-image".getBytes());

        given(userService.findById(1L)).willReturn(user1);
        given(userService.updateCoverImage(eq(1L), any(String.class))) // Sửa thành updateCoverImage
                .willThrow(new RuntimeException("Lỗi IO khi lưu ảnh bìa")); // Ví dụ lỗi

        mockMvc.perform(multipart("/users/1/cover-image") // Sửa URL
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());
    }
}
