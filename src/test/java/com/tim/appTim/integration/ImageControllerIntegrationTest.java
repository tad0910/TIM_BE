package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.UserImage;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    // Giả lập service
    @MockBean
    private UserImageService userImageService;

    // UserService thật để kiểm tra @userService.isSelf
    @Autowired
    private UserService userService;

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
        testImage.setUserId(1L); // Lỗi thiết kế (như đã nói)
        testImage.setImageUrl("/uploads/image-cua-user-1.jpg");
    }

    // --- Test Lấy danh sách ảnh (GET) ---
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void getAllImages_WhenImagesExist_ShouldReturn200() throws Exception {
        // Giả lập service trả về 1 ảnh
        when(userImageService.findAllByUserId(1L)).thenReturn(List.of(testImage));

        mockMvc.perform(get(BASE_URL + "/1/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].userId").value(1L));
    }

    // --- Test Xóa ảnh (DELETE) ---
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenUserIsSelfAndOwner_ShouldReturn200() throws Exception {
        // Giả lập service tìm thấy ảnh
        when(userImageService.findById(100L)).thenReturn(testImage);
        // Giả lập service xóa thành công
        doNothing().when(userImageService).delete(100L);

        mockMvc.perform(delete(BASE_URL + "/1/image/100"))
                .andExpect(status().isOk())
                .andExpect(content().string("Xóa ảnh thành công"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenUserIsSelfButNotOwner_ShouldReturn404() throws Exception {
        // User 2 cố xóa ảnh 100 (thuộc User 1)

        // Giả lập service tìm thấy ảnh
        when(userImageService.findById(100L)).thenReturn(testImage);
        // (Lưu ý: testImage.getUserId() là 1L)

        // User 2 (ID 2) đang cố xóa ảnh 100 của User 1
        // @PreAuthorize("...isSelf...") (2L, 2L) -> PASS
        // Logic trong Controller: if (!userImage.getUserId().equals(userId)) -> if (!1L.equals(2L)) -> true
        // Controller ném ResourceNotFoundException
        mockMvc.perform(delete(BASE_URL + "/2/image/100"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteImage_WhenUserIsNotSelf_ShouldReturn403() throws Exception {
        // User 1 cố gắng xóa ảnh qua API của User 2 (bị @PreAuthorize chặn)
        mockMvc.perform(delete(BASE_URL + "/2/image/100"))
                .andExpect(status().isForbidden());
    }
}