package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.service.KeycloakSyncService;
import com.tim.appTim.service.PostService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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

    @MockBean
    private PostService postService;

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
        void testUpdateComment_WhenUserIsPostOwner_ShouldReturn200() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Chủ post cập nhật comment.";

        mockMvc.perform(put("/comments/" + commentId)
                        .param("content", updatedContent))
                .andExpect(status().isOk()) 
                .andExpect(jsonPath("$.content").value(updatedContent));
        }

        @Test
        @WithUserDetails(value = "stranger_user", userDetailsServiceBeanName = "userService")
        void testUpdateComment_WhenUserIsStranger_ShouldReturn403() throws Exception {
        Long commentId = 20L;
        String updatedContent = "Người lạ cố sửa comment.";

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
    @WithUserDetails(value = "stranger_user", userDetailsServiceBeanName = "userService")
        void testDeleteComment_WhenUserIsStranger_ShouldReturn403() throws Exception {
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

    // ========== GET /comments/posts/{postId} ==========
    @Test
        @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
        void getCommentsByPostId_WhenPostExists_ShouldReturn200() throws Exception {
        Long postId = 10L;   

        mockMvc.perform(get("/comments/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content[0].id").value(20))
                .andExpect(jsonPath("$.content[0].username").value("another_user"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

    @Test
        @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
        void getCommentsByPostId_WhenPostDoesNotExist_ShouldReturn200WithEmptyList() throws Exception {
        Long postId = 999L;

        mockMvc.perform(get("/comments/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

    @Test
        @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
        void getCommentsByPostId_WhenPostHasNoComments_ShouldReturn200WithEmptyList() throws Exception {
        Long postId = 11L;  

        mockMvc.perform(get("/comments/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

    // ========== POST /comments/posts/{postId} - Additional tests ==========
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createComment_WithFiles_ShouldReturn200() throws Exception {
        Long postId = 10L;
        String content = "Comment với file đính kèm";

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test_image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/comments/posts/" + postId)
                        .file(file)
                        .param("content", content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files[0].fileName").value("test_image.jpg"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createComment_WithEmotion_ShouldReturn200() throws Exception {
        Long postId = 10L;
        String content = "Comment với emotion";
        String emotion = "like";

        mockMvc.perform(post("/comments/posts/" + postId)
                        .param("content", content)
                        .param("emotion", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.emotion").value(emotion)); 
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createComment_WhenPostDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentPostId = 999L;
        String content = "Comment cho post không tồn tại";

        mockMvc.perform(post("/comments/posts/" + nonExistentPostId)
                        .param("content", content))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createComment_WithInvalidEmotion_ShouldReturn400() throws Exception {
        Long postId = 10L;
        String content = "Comment với emotion không hợp lệ";
        String invalidEmotion = "invalid_emotion";

        mockMvc.perform(post("/comments/posts/" + postId)
                        .param("content", content)
                        .param("emotion", invalidEmotion))
                .andExpect(status().isBadRequest());
    }

    // ========== POST /comments/{commentId}/replies - createReplyComment ==========
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createReplyComment_WhenUserIsAuthenticated_ShouldReturn200() throws Exception {
        Long commentId = 20L;
        String content = "Đây là một reply comment";

        mockMvc.perform(post("/comments/" + commentId + "/replies")
                        .param("content", content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.commentId").value(commentId));
    }

    @Test
    void createReplyComment_WhenUserIsAnonymous_ShouldReturn401() throws Exception {
        Long commentId = 20L;
        String content = "Reply không được phép";

        mockMvc.perform(post("/comments/" + commentId + "/replies")
                        .param("content", content))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createReplyComment_WithFiles_ShouldReturn200() throws Exception {
        Long commentId = 20L;
        String content = "Reply với file đính kèm";

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "reply_document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "test pdf content".getBytes()
        );

        mockMvc.perform(multipart("/comments/" + commentId + "/replies")
                        .file(file)
                        .param("content", content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files[0].fileName").value("reply_document.pdf"));
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createReplyComment_WithEmotion_ShouldReturn200() throws Exception {
        Long commentId = 20L;
        String content = "Reply với emotion";
        String emotion = "love";

        mockMvc.perform(post("/comments/" + commentId + "/replies")
                        .param("content", content)
                        .param("emotion", emotion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content))
                .andExpect(jsonPath("$.emotion").value(emotion)); 
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createReplyComment_WhenCommentDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentCommentId = 999L;
        String content = "Reply cho comment không tồn tại";

        mockMvc.perform(post("/comments/" + nonExistentCommentId + "/replies")
                        .param("content", content))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void createReplyComment_WithInvalidEmotion_ShouldReturn400() throws Exception {
        Long commentId = 20L;
        String content = "Reply với emotion không hợp lệ";
        String invalidEmotion = "invalid_emotion";

        mockMvc.perform(post("/comments/" + commentId + "/replies")
                        .param("content", content)
                        .param("emotion", invalidEmotion))
                .andExpect(status().isBadRequest());
    }

    // ========== GET /comments/{commentId}/replies ==========
    @Test
        @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
        void getReplyCommentsByCommentId_WhenCommentExists_ShouldReturn200() throws Exception {
        Long commentId = 20L;  

        mockMvc.perform(get("/comments/{commentId}/replies", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content[0].id").value(30))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

    @Test
        @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
        void getReplyCommentsByCommentId_WhenCommentDoesNotExist_ShouldReturn200WithEmptyList() throws Exception {
        Long commentId = 999L;

        mockMvc.perform(get("/comments/{commentId}/replies", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

    @Test
        @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
        void getReplyCommentsByCommentId_WhenCommentHasNoReplies_ShouldReturn200WithEmptyList() throws Exception {
        Long commentId = 21L;  

        mockMvc.perform(get("/comments/{commentId}/replies", commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

    // ========== DELETE /comments/{commentId} - Additional tests ==========
    @Test
    @WithUserDetails(value = "another_user", userDetailsServiceBeanName = "userService")
    void deleteComment_WhenCommentDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentCommentId = 999L;

        mockMvc.perform(delete("/comments/" + nonExistentCommentId))
                .andExpect(status().isNotFound());
    }

    // ========== DELETE /comments/replies/{replyCommentId} - Additional tests ==========
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void deleteReply_WhenReplyDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentReplyId = 999L;

        mockMvc.perform(delete("/comments/replies/" + nonExistentReplyId))
                .andExpect(status().isNotFound());
    }

    // ========== PUT /comments/replies/{replyCommentId} - Additional tests ==========
    @Test
    @WithUserDetails(value = "post_owner", userDetailsServiceBeanName = "userService")
    void updateReply_WhenReplyDoesNotExist_ShouldReturn404() throws Exception {
        Long nonExistentReplyId = 999L;
        String updatedContent = "Cập nhật reply không tồn tại";

        mockMvc.perform(put("/comments/replies/" + nonExistentReplyId)
                        .param("content", updatedContent))
                .andExpect(status().isNotFound());
    }
}
