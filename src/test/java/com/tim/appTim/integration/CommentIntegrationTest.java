package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.service.KeycloakSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/test-data.sql")
@ActiveProfiles("test")
public class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakSyncService keycloakSyncService;

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createComment_WhenUserIsAuthenticated_ShouldReturnCorrectStatus() throws Exception {
        Long postId = 10L;
        String noiDungBinhLuan = "Đây là một bình luận test tuyệt vời!";

        mockMvc.perform(post("/comments/posts/" + postId)
                        .param("content", noiDungBinhLuan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(noiDungBinhLuan))
                .andExpect(jsonPath("$.userId").value(2L));
    }

    @Test
    void createComment_WhenUserIsAnonymous_ShouldReturn401() throws Exception {
        Long postId = 10L;
        String noiDungBinhLuan = "Bình luận này sẽ thất bại";

        mockMvc.perform(post("/comments/posts/" + postId)
                        .param("content", noiDungBinhLuan))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testUpdateComment_WhenUserIsOwner_ShouldReturn200AndUpdatedComment() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Nội dung comment đã được cập nhật.";

        mockMvc.perform(put("/comments/" + commentId)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.userId").value(2L));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdateComment_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Cố gắng sửa comment người khác.";

        mockMvc.perform(put("/comments/" + commentId)
                        .param("content", updatedContent))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testUpdateComment_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Admin cập nhật comment.";

        mockMvc.perform(put("/comments/" + commentId)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(updatedContent));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testUpdateComment_WhenCommentNotFound_ShouldReturn404() throws Exception {
        Long nonExistentCommentId = 999L;
        String updatedContent = "Sửa comment không tồn tại.";

        mockMvc.perform(put("/comments/" + nonExistentCommentId)
                        .param("content", updatedContent))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testDeleteComment_WhenUserIsOwner_ShouldReturn200() throws Exception {
        Long commentId = 20L;

        mockMvc.perform(delete("/comments/" + commentId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testDeleteComment_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long commentId = 20L;

        mockMvc.perform(delete("/comments/" + commentId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testDeleteComment_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long commentId = 20L;

        mockMvc.perform(delete("/comments/" + commentId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdateReply_WhenUserIsOwner_ShouldReturn200AndUpdatedReply() throws Exception {
        Long replyId = 30L;
        String updatedContent = "Nội dung reply đã được cập nhật.";

        mockMvc.perform(put("/comments/replies/" + replyId)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(replyId))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testUpdateReply_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long replyId = 30L;
        String updatedContent = "Cố gắng sửa reply người khác.";

        mockMvc.perform(put("/comments/replies/" + replyId)
                        .param("content", updatedContent))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testUpdateReply_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long replyId = 30L;
        String updatedContent = "Admin cập nhật reply.";

        mockMvc.perform(put("/comments/replies/" + replyId)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(updatedContent));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testDeleteReply_WhenUserIsOwner_ShouldReturn200() throws Exception {
        Long replyId = 30L;

        mockMvc.perform(delete("/comments/replies/" + replyId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testDeleteReply_WhenUserIsNotOwner_ShouldReturn403() throws Exception {
        Long replyId = 30L;

        mockMvc.perform(delete("/comments/replies/" + replyId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin_user", userDetailsServiceBeanName = "userService")
    void testDeleteReply_WhenUserIsAdmin_ShouldReturn200() throws Exception {
        Long replyId = 30L;

        mockMvc.perform(delete("/comments/replies/" + replyId))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void testUpdateComment_ReplaceFiles_ShouldReturn200AndNewFiles() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Nội dung đã được cập nhật và thay ảnh.";

        MockMultipartFile newMockFile = new MockMultipartFile(
                "files",
                "new_image.png",
                MediaType.IMAGE_PNG_VALUE,
                "nội dung file ảnh mới".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/comments/" + commentId)
                        .file(newMockFile)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files[0].fileType").value("IMAGE"))
                .andExpect(jsonPath("$.files[0].fileName").value("new_image.png"));
    }

    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void testUpdateReply_ReplaceFiles_ShouldReturn200AndNewFiles() throws Exception {
        Long replyId = 30L;
        String updatedContent = "Reply đã được cập nhật và thay ảnh.";

        MockMultipartFile newMockFile = new MockMultipartFile(
                "files",
                "reply_file.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "nội dung file text mới".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/comments/replies/" + replyId)
                        .file(newMockFile)
                        .param("content", updatedContent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(replyId))
                .andExpect(jsonPath("$.content").value(updatedContent))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files[0].fileType").value("DOCUMENT"))
                .andExpect(jsonPath("$.files[0].fileName").value("reply_file.txt"));
    }
}
