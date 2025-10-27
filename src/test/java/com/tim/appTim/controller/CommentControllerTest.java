//package com.tim.appTim.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.tim.appTim.config.TestSecurityConfig;
//import com.tim.appTim.dto.CommentDTO;
//import com.tim.appTim.dto.ReplyCommentDTO; // Cần import ReplyCommentDTO
//import com.tim.appTim.entity.Comment;
//import com.tim.appTim.entity.ReplyComment; // Cần import ReplyComment cho Enum
//import com.tim.appTim.entity.User;
//import com.tim.appTim.service.CommentService;
//import com.tim.appTim.service.UserService;
//import com.tim.appTim.util.JwtUtil;
//import com.tim.appTim.service.KeycloakIntrospectionService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doThrow;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(controllers = CommentController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
//@Import(TestSecurityConfig.class)
//class CommentControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private CommentService commentService;
//
//    @MockBean
//    private UserService userService;
//
//    // Mock beans cần thiết cho security filter chain
//    @MockBean
//    private JwtUtil jwtUtil;
//    @MockBean
//    private KeycloakIntrospectionService keycloakIntrospectionService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private User mockUser;
//    private CommentDTO commentDTO1;
//    private ReplyCommentDTO replyDTO1; // Thêm ReplyDTO mẫu
//
//    @BeforeEach
//    void setUp() {
//        // --- Setup User ---
//        mockUser = new User();
//        mockUser.setId(1L);
//        mockUser.setUsername("testuser");
//        mockUser.setEmail("test@example.com");
//        given(userService.findByUsernameOrEmail(anyString())).willReturn(mockUser);
//
//        // --- Setup CommentDTO ---
//        // (Sử dụng constructor của CommentDTO bạn cung cấp)
//        commentDTO1 = new CommentDTO(
//                10L,                          // id
//                mockUser.getId(),             // userId
//                mockUser.getUsername(),       // username
//                "Nội dung bình luận mẫu",    // content
//                Comment.Emotion.LIKE.name(),  // emotion (lấy name từ enum)
//                null,                         // fileId (có thể null)
//                LocalDateTime.now(),          // createdAt
//                Collections.emptyList()       // replyComments (danh sách rỗng ban đầu)
//        );
//
//        // --- Setup ReplyCommentDTO ---
//        // (Giả sử ReplyCommentDTO có constructor tương tự hoặc dùng setters)
//        replyDTO1 = new ReplyCommentDTO(/* Dữ liệu mẫu */);
//        replyDTO1.setId(100L);
//        replyDTO1.setUserId(mockUser.getId());
//        replyDTO1.setUsername(mockUser.getUsername());
//        replyDTO1.setContent("Nội dung trả lời mẫu");
//        replyDTO1.setEmotion(ReplyComment.Emotion.HAHA.name()); // Dùng enum của ReplyComment
//        replyDTO1.setCreatedAt(LocalDateTime.now());
//    }
//
//    // ==========================================================
//    // == Tests cho Comment API (/comments) ==
//    // ==========================================================
//
//    // ----------------------------------------------------------
//    // 1️⃣ POST /comments/posts/{postId}
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:create"})
//    void createComment_WhenValid_ShouldReturn200AndComment() throws Exception {
//        Long postId = 5L;
//        String content = "Bình luận mới!";
//        String emotion = "LIKE";
//
//        // Tạo DTO mới cho kết quả trả về (để khớp emotion)
//        CommentDTO createdComment = new CommentDTO(11L, mockUser.getId(), mockUser.getUsername(), content, emotion, null, LocalDateTime.now(), Collections.emptyList());
//
//        given(commentService.createComment(eq(postId), eq(mockUser.getId()), eq(content), eq(Comment.Emotion.LIKE), isNull()))
//                .willReturn(createdComment);
//
//        mockMvc.perform(post("/comments/posts/" + postId)
//                        .param("content", content)
//                        .param("emotion", emotion)) // Gửi param
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(11L))
//                .andExpect(jsonPath("$.content").value(content))
//                .andExpect(jsonPath("$.emotion").value(emotion));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:create"})
//    void createComment_WhenInvalidEmotion_ShouldReturn400() throws Exception {
//        mockMvc.perform(post("/comments/posts/5")
//                        .param("content", "Nội dung")
//                        .param("emotion", "INVALID"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền
//    void createComment_WhenNoPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(post("/comments/posts/5")
//                        .param("content", "Nội dung")
//                        .param("emotion", "LIKE"))
//                .andExpect(status().isForbidden());
//    }
//
//    // ----------------------------------------------------------
//    // 2️⃣ GET /comments/posts/{postId}
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser // Endpoint công khai
//    void getCommentsByPostId_ShouldReturn200AndList() throws Exception {
//        Long postId = 5L;
//        List<CommentDTO> comments = Collections.singletonList(commentDTO1);
//        given(commentService.getCommentsByPostId(postId)).willReturn(comments);
//
//        mockMvc.perform(get("/comments/posts/" + postId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].id").value(commentDTO1.getId()));
//    }
//
//    // ----------------------------------------------------------
//    // 3️⃣ PUT /comments/{commentId} (Chỉ test admin)
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:update_all"})
//    void updateComment_WhenAdminAndValid_ShouldReturn200AndUpdatedComment() throws Exception {
//        Long commentId = 10L;
//        String updatedContent = "Nội dung đã sửa";
//        String updatedEmotion = "HAHA";
//
//        // Tạo DTO mới cho kết quả
//        CommentDTO updatedComment = new CommentDTO(commentId, mockUser.getId(), mockUser.getUsername(), updatedContent, updatedEmotion, null, LocalDateTime.now(), Collections.emptyList());
//
//        given(commentService.updateComment(eq(commentId), eq(mockUser.getId()), eq(updatedContent), eq(Comment.Emotion.HAHA)))
//                .willReturn(updatedComment);
//
//        mockMvc.perform(put("/comments/" + commentId)
//                        .param("content", updatedContent)
//                        .param("emotion", updatedEmotion))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(commentId))
//                .andExpect(jsonPath("$.content").value(updatedContent))
//                .andExpect(jsonPath("$.emotion").value(updatedEmotion));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền admin
//    void updateComment_WhenNoAdminPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(put("/comments/10")
//                        .param("content", "abc")
//                        .param("emotion", "LIKE"))
//                .andExpect(status().isForbidden());
//    }
//
//    // ----------------------------------------------------------
//    // 4️⃣ DELETE /comments/{commentId} (Chỉ test admin)
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:delete_all"})
//    void deleteComment_WhenAdmin_ShouldReturn200() throws Exception {
//        Long commentId = 10L;
//        doNothing().when(commentService).deleteComment(commentId, mockUser.getId());
//
//        mockMvc.perform(delete("/comments/" + commentId))
//                .andExpect(status().isOk())
//                .andExpect(content().string("Comment deleted successfully"));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền admin
//    void deleteComment_WhenNoAdminPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(delete("/comments/10"))
//                .andExpect(status().isForbidden());
//    }
//
//    // ==========================================================
//    // == Tests cho Reply Comment API (/comments/{commentId}/replies, /comments/replies/{replyId}) ==
//    // ==========================================================
//
//    // ----------------------------------------------------------
//    // 5️⃣ POST /comments/{commentId}/replies
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:create"})
//    void createReplyComment_WhenValid_ShouldReturn200AndReply() throws Exception {
//        Long commentId = 10L;
//        String content = "Trả lời bình luận!";
//        String emotion = "HAHA";
//
//        // Sử dụng replyDTO1 đã tạo trong setUp
//        given(commentService.createReplyComment(eq(commentId), eq(mockUser.getId()), eq(content), eq(ReplyComment.Emotion.HAHA), isNull()))
//                .willReturn(replyDTO1);
//
//        mockMvc.perform(post("/comments/" + commentId + "/replies")
//                        .param("content", content)
//                        .param("emotion", emotion))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(replyDTO1.getId()))
//                .andExpect(jsonPath("$.content").value(replyDTO1.getContent()))
//                .andExpect(jsonPath("$.emotion").value(replyDTO1.getEmotion()));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:create"})
//    void createReplyComment_WhenInvalidEmotion_ShouldReturn400() throws Exception {
//        mockMvc.perform(post("/comments/10/replies")
//                        .param("content", "Nội dung")
//                        .param("emotion", "INVALID"))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền
//    void createReplyComment_WhenNoPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(post("/comments/10/replies")
//                        .param("content", "Nội dung")
//                        .param("emotion", "HAHA"))
//                .andExpect(status().isForbidden());
//    }
//
//    // ----------------------------------------------------------
//    // 6️⃣ GET /comments/{commentId}/replies
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser // Endpoint công khai
//    void getReplyCommentsByCommentId_ShouldReturn200AndList() throws Exception {
//        Long commentId = 10L;
//        List<ReplyCommentDTO> replies = Collections.singletonList(replyDTO1);
//        given(commentService.getReplyCommentsByCommentId(commentId)).willReturn(replies);
//
//        mockMvc.perform(get("/comments/" + commentId + "/replies"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].id").value(replyDTO1.getId()));
//    }
//
//    // ----------------------------------------------------------
//    // 7️⃣ PUT /comments/replies/{replyCommentId} (Chỉ test admin)
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:update_all"})
//    void updateReplyComment_WhenAdminAndValid_ShouldReturn200AndUpdatedReply() throws Exception {
//        Long replyCommentId = 100L;
//        String updatedContent = "Trả lời đã sửa";
//        String updatedEmotion = "WOW";
//
//        // Tạo DTO mới cho kết quả
//        ReplyCommentDTO updatedReply = new ReplyCommentDTO(/* Dữ liệu mới */);
//        updatedReply.setId(replyCommentId);
//        updatedReply.setContent(updatedContent);
//        updatedReply.setEmotion(updatedEmotion); // Giả sử là String
//
//        given(commentService.updateReplyComment(eq(mockUser.getId()), eq(replyCommentId), eq(updatedContent), eq(ReplyComment.Emotion.WOW)))
//                .willReturn(updatedReply);
//
//        mockMvc.perform(put("/comments/replies/" + replyCommentId)
//                        .param("content", updatedContent)
//                        .param("emotion", updatedEmotion))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(replyCommentId))
//                .andExpect(jsonPath("$.content").value(updatedContent))
//                .andExpect(jsonPath("$.emotion").value(updatedEmotion));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền admin
//    void updateReplyComment_WhenNoAdminPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(put("/comments/replies/100")
//                        .param("content", "abc")
//                        .param("emotion", "WOW"))
//                .andExpect(status().isForbidden());
//    }
//
//    // ----------------------------------------------------------
//    // 8️⃣ DELETE /comments/replies/{replyCommentId} (Chỉ test admin)
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"comment:delete_all"})
//    void deleteReplyComment_WhenAdmin_ShouldReturn200() throws Exception {
//        Long replyCommentId = 100L;
//        doNothing().when(commentService).deleteReplyComment(mockUser.getId(), replyCommentId);
//
//        mockMvc.perform(delete("/comments/replies/" + replyCommentId))
//                .andExpect(status().isOk())
//                .andExpect(content().string("Reply comment deleted successfully"));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền admin
//    void deleteReplyComment_WhenNoAdminPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(delete("/comments/replies/100"))
//                .andExpect(status().isForbidden());
//    }
//}