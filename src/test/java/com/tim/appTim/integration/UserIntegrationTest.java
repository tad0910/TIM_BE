package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.ClassMember;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.UserImage;
import com.tim.appTim.service.ClassService;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.UserImageService;
import com.tim.appTim.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @SpyBean
    private UserImageService userImageService;

    @MockBean
    private ClassService classService;

    @SpyBean
    private UserService userService;

    private final String BASE_URL = "/users";
    private User testUser1;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testUser1 = userService.findById(1L);
    }

    @Test
    @WithMockUser(username = "admin_user", authorities = "user:read_all")
    void getAllUsers_WhenAdmin_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllUsers_WhenNotAdmin_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getById_WhenUserIsSelf_ShouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("post_owner"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getById_WhenUserIsOther_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void updateUser_WhenUserIsSelf_ShouldReturn200() throws Exception {
        testUser1.setFirstName("Updated First");

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated First"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void uploadProfileImage_WhenUserIsSelf_ShouldReturn200() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        UserImage savedImage = new UserImage();
        doReturn(savedImage).when(userImageService).save(any(UserImage.class));
        testUser1.setProfileImage("/uploads/new-image.jpg");
        doReturn(testUser1).when(userService).updateProfileImage(eq(1L), anyString());
        mockMvc.perform(multipart(BASE_URL + "/1/profile-image").file(mockFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ảnh đại diện đã được cập nhật thành công"));
    }



    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void updateProfileImage_ByUrl_WhenUserIsSelf_ShouldReturn200() throws Exception {
        String newImageUrl = "http://example.com/new.jpg";
        testUser1.setProfileImage(newImageUrl);
        doReturn(testUser1).when(userService).updateProfileImage(eq(1L), eq(newImageUrl));

        mockMvc.perform(put(BASE_URL + "/1/profile-image")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("imageUrl", newImageUrl))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.profileImage").value(newImageUrl));
    }


    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getUserClasses_WhenCalled_ShouldReturn200() throws Exception {
        ClassMember member = new ClassMember();
        member.setClassId(10L);
        member.setRole(ClassMember.Role.sinh_vien);

        when(classService.getUserClasses(1L)).thenReturn(List.of(member));

        mockMvc.perform(get(BASE_URL + "/1/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalClasses").value(1))
                .andExpect(jsonPath("$.classes[0].role").value("sinh_vien"));
    }
}