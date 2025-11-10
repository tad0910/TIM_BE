package com.tim.appTim.integration;

import com.tim.appTim.entity.UserImage;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.PostService;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class ImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @MockBean
    private UserImageService userImageService;

    @Autowired
    @SuppressWarnings("unused")
    private UserService userService;

    @MockBean
    private PostService postService;

    private final String BASE_URL = "/api/users";
    private UserImage testImage;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testImage = new UserImage();
        testImage.setId(100L);
        testImage.setUserId(1L); 
        testImage.setImageUrl("/uploads/image-cua-user-1.jpg");
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllImages_WhenImagesExist_ShouldReturn200() throws Exception {
        when(userImageService.findAllByUserId(1L)).thenReturn(List.of(testImage));

        mockMvc.perform(get(BASE_URL + "/1/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenUserIsSelfAndOwner_ShouldReturn200() throws Exception {
        when(userImageService.findById(100L)).thenReturn(testImage);
        doNothing().when(userImageService).delete(100L);

        mockMvc.perform(delete(BASE_URL + "/1/image/100"))
                .andExpect(status().isOk())
                .andExpect(content().string("Xóa ảnh thành công"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenUserIsSelfButNotOwner_ShouldReturn404() throws Exception {
        when(userImageService.findById(100L)).thenReturn(testImage);
        mockMvc.perform(delete(BASE_URL + "/2/image/100"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/2/image/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        when(userImageService.findById(100L)).thenReturn(testImage);
        doNothing().when(userImageService).delete(100L);

        mockMvc.perform(delete(BASE_URL + "/1/image/100"))
                .andExpect(status().isOk())
                .andExpect(content().string("Xóa ảnh thành công"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenImageDoesNotExist_ShouldReturn404() throws Exception {
        when(userImageService.findById(999L)).thenReturn(null);

        mockMvc.perform(delete(BASE_URL + "/1/image/999"))
                .andExpect(status().isNotFound());
    }

    // ========== GET /api/users/{userId}/image - Additional tests ==========
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllImages_WhenNoImagesExist_ShouldReturn404() throws Exception {
        when(userImageService.findAllByUserId(2L)).thenReturn(List.of());
        mockMvc.perform(get(BASE_URL + "/2/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllImages_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
        when(userImageService.findAllByUserId(999L)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/999/image"))
                .andExpect(status().isNotFound());
    }

    // ========== POST /api/users/{userId}/image - uploadImage ==========
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void uploadImage_WhenUserIsSelf_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        when(userImageService.save(any(UserImage.class))).thenAnswer(invocation -> {
            UserImage img = invocation.getArgument(0);
            img.setId(200L);
            return img;
        });

        mockMvc.perform(multipart(BASE_URL + "/1/image")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tải ảnh thành công")));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void uploadImage_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "admin-upload.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "admin upload content".getBytes()
        );

        when(userImageService.save(any(UserImage.class))).thenAnswer(invocation -> {
            UserImage img = invocation.getArgument(0);
            img.setId(201L);
            return img;
        });

        mockMvc.perform(multipart(BASE_URL + "/1/image")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tải ảnh thành công")));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void uploadImage_WhenFileIsEmpty_ShouldReturn400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart(BASE_URL + "/1/image")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File tải lên bị trống"));
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void uploadImage_WhenUserDoesNotExist_ShouldReturn404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test content".getBytes()
        );

        mockMvc.perform(multipart(BASE_URL + "/999/image")
                        .file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void uploadImage_WhenUserIsNotSelfAndNotAdmin_ShouldReturn403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test content".getBytes()
        );

        mockMvc.perform(multipart(BASE_URL + "/1/image")
                        .file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void uploadImage_WithDifferentFileTypes_ShouldReturn200() throws Exception {
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "png content".getBytes()
        );

        when(userImageService.save(any(UserImage.class))).thenAnswer(invocation -> {
            UserImage img = invocation.getArgument(0);
            img.setId(202L);
            return img;
        });

        mockMvc.perform(multipart(BASE_URL + "/1/image")
                        .file(pngFile))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tải ảnh thành công")));
    }
}