package com.tim.appTim.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.config.TestSecurityConfig;
import com.tim.appTim.dto.ReactionDTO;
import com.tim.appTim.entity.Reaction;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.ReactionService;
import com.tim.appTim.service.UserService;
import com.tim.appTim.util.JwtUtil; // Giữ lại từ UserControllerTest
import com.tim.appTim.service.KeycloakIntrospectionService; // Giữ lại từ UserControllerTest
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReactionController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(TestSecurityConfig.class)
class ReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReactionService reactionService;

    @MockBean
    private UserService userService;

    // Các MockBean cần thiết cho Security Filter (giữ lại từ UserControllerTest)
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private KeycloakIntrospectionService keycloakIntrospectionService;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;
    private ReactionDTO reactionDTO;

    @BeforeEach
    void setUp() {
        // --- Setup User giả lập ---
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");

        // Giả lập userService trả về user này khi controller gọi getUserFromAuthentication
        given(userService.findByUsernameOrEmail(anyString())).willReturn(mockUser);

        Long reactionId = 1L;
        Long userId = mockUser.getId();
        String username = mockUser.getUsername();
        String userAvatar = null; // Hoặc "avatar/default.png"
        String emotionString = "like";
        LocalDateTime timestamp = LocalDateTime.now();

        reactionDTO = new ReactionDTO(reactionId, userId, username, userAvatar, emotionString, timestamp);

    }

    // ==========================================================
    // == Tests cho API Reaction trên Post (/reactions/posts) ==
    // ==========================================================

    // ----------------------------------------------------------
    // 1️⃣ POST /reactions/posts/{postId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:create"})
    void createOrUpdateReaction_WhenValid_ShouldReturn200AndReaction() throws Exception {
        given(reactionService.createOrUpdateReaction(eq(10L), eq(mockUser.getId()), eq(Reaction.EmotionType.like)))
                .willReturn(reactionDTO);

        mockMvc.perform(post("/reactions/posts/10")
                        .param("emotionType", "like")) // Gửi emotionType như một request param
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(mockUser.getId()))
                .andExpect(jsonPath("$.emotionType").value("like"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:create"})
    void createOrUpdateReaction_WhenInvalidEmotionType_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/reactions/posts/10")
                        .param("emotionType", "invalid_emotion")) // Gửi emotionType không hợp lệ
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid emotion type: invalid_emotion"));
    }

    @Test
    @WithMockUser(username = "testuser") // Không có quyền reaction:create
    void createOrUpdateReaction_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/reactions/posts/10")
                        .param("emotionType", "like"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:create"})
    void createOrUpdateReaction_WhenServiceThrowsError_ShouldReturn500() throws Exception {
        given(reactionService.createOrUpdateReaction(eq(10L), eq(mockUser.getId()), eq(Reaction.EmotionType.love)))
                .willThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/reactions/posts/10")
                        .param("emotionType", "love"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error while creating/updating reaction: Database error"));
    }


    // ----------------------------------------------------------
    // 2️⃣ GET /reactions/posts/{postId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser // Endpoint này không yêu cầu quyền
    void getReactionsByPostId_ShouldReturn200AndReactionList() throws Exception {
        List<ReactionDTO> reactions = Arrays.asList(reactionDTO);
        given(reactionService.getReactionsByPostId(10L)).willReturn(reactions);

        mockMvc.perform(get("/reactions/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].emotionType").value("like"));
    }

    @Test
    @WithMockUser
    void getReactionsByPostId_WhenNoReactions_ShouldReturn200AndEmptyList() throws Exception {
        given(reactionService.getReactionsByPostId(11L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/reactions/posts/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty()); // Mong đợi một danh sách rỗng
    }


    // ----------------------------------------------------------
    // 3️⃣ DELETE /reactions/posts/{postId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:delete"})
    void deleteReaction_WhenValid_ShouldReturn200() throws Exception {
        // Không cần mock gì cả, vì service trả về void và controller trả về String cố định
        mockMvc.perform(delete("/reactions/posts/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Reaction deleted successfully"));
    }

    @Test
    @WithMockUser(username = "testuser") // Không có quyền reaction:delete
    void deleteReaction_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/reactions/posts/10"))
                .andExpect(status().isForbidden());
    }


    // ----------------------------------------------------------
    // 4️⃣ GET /reactions/posts/{postId}/count/{emotionType}
    // ----------------------------------------------------------
    @Test
    @WithMockUser // Endpoint này không yêu cầu quyền
    void countReactionsByType_WhenValid_ShouldReturn200AndCount() throws Exception {
        // Sửa LIKE thành like (viết thường) ở đây
        given(reactionService.countReactionsByType(10L, Reaction.EmotionType.like)).willReturn(5L);

        // API endpoint vẫn nhận chữ hoa (do controller có .toUpperCase())
        mockMvc.perform(get("/reactions/posts/10/count/LIKE"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @WithMockUser
    void countReactionsByType_WhenInvalidEmotionType_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/reactions/posts/10/count/INVALID")) // Gửi emotionType không hợp lệ
                .andExpect(status().isBadRequest());
    }

    // ==========================================================
    // == Tests cho API Reaction trên Comment (/reactions/comments) ==
    // ==========================================================

    // ----------------------------------------------------------
    // 5️⃣ POST /reactions/comments/{commentId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:create"})
    void createOrUpdateCommentReaction_WhenValid_ShouldReturn200AndReaction() throws Exception {
        Long reactionId = 2L;
        Long userId = mockUser.getId();
        String username = mockUser.getUsername();
        String userAvatar = null; // Hoặc "avatar/test.jpg"
        String emotionString = "love";
        LocalDateTime timestamp = LocalDateTime.now();

        // Thêm userAvatar vào constructor
        ReactionDTO loveReactionDTO = new ReactionDTO(reactionId, userId, username, userAvatar, emotionString, timestamp);

        given(reactionService.createOrUpdateCommentReaction(eq(20L), eq(mockUser.getId()), eq(Reaction.EmotionType.love)))
                .willReturn(loveReactionDTO);

        mockMvc.perform(post("/reactions/comments/20")
                        .param("emotionType", "love"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reactionId))
                .andExpect(jsonPath("$.emotionType").value("love"));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:create"})
    void createOrUpdateCommentReaction_WhenInvalidEmotionType_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/reactions/comments/20")
                        .param("emotionType", "bad_emotion"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid emotion type: bad_emotion"));
    }

    @Test
    @WithMockUser(username = "testuser") // Thiếu quyền
    void createOrUpdateCommentReaction_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/reactions/comments/20")
                        .param("emotionType", "love"))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------
    // 6️⃣ GET /reactions/comments/{commentId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    void getReactionsByCommentId_ShouldReturn200AndList() throws Exception {
        List<ReactionDTO> reactions = Collections.singletonList(reactionDTO);
        given(reactionService.getReactionsByCommentId(20L)).willReturn(reactions);

        mockMvc.perform(get("/reactions/comments/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    // ----------------------------------------------------------
    // 7️⃣ DELETE /reactions/comments/{commentId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:delete"})
    void deleteCommentReaction_WhenValid_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/reactions/comments/20"))
                .andExpect(status().isOk())
                .andExpect(content().string("Reaction deleted successfully"));
    }

    @Test
    @WithMockUser(username = "testuser") // Thiếu quyền
    void deleteCommentReaction_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/reactions/comments/20"))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------
    // 8️⃣ GET /reactions/comments/{commentId}/count/{emotionType}
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    void countCommentReactionsByType_WhenValid_ShouldReturn200AndCount() throws Exception {
        given(reactionService.countCommentReactionsByType(20L, Reaction.EmotionType.haha)).willReturn(3L);

        // Giả sử bạn đã sửa controller dùng toLowerCase()
        mockMvc.perform(get("/reactions/comments/20/count/haha"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @WithMockUser
    void countCommentReactionsByType_WhenInvalidEmotionType_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/reactions/comments/20/count/INVALID"))
                .andExpect(status().isBadRequest());
    }


    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:create"})
    void createOrUpdateReplyReaction_WhenInvalidEmotionType_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/reactions/replies/30")
                        .param("emotionType", "weird_emotion"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid emotion type: weird_emotion"));
    }

    @Test
    @WithMockUser(username = "testuser") // Thiếu quyền
    void createOrUpdateReplyReaction_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(post("/reactions/replies/30")
                        .param("emotionType", "wow"))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------
    // 🔟 GET /reactions/replies/{replyCommentId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    void getReactionsByReplyCommentId_ShouldReturn200AndList() throws Exception {
        List<ReactionDTO> reactions = Collections.singletonList(reactionDTO);
        given(reactionService.getReactionsByReplyCommentId(30L)).willReturn(reactions);

        mockMvc.perform(get("/reactions/replies/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    // ----------------------------------------------------------
    // 1️⃣1️⃣ DELETE /reactions/replies/{replyCommentId}
    // ----------------------------------------------------------
    @Test
    @WithMockUser(username = "testuser", authorities = {"reaction:delete"})
    void deleteReplyCommentReaction_WhenValid_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/reactions/replies/30"))
                .andExpect(status().isOk())
                .andExpect(content().string("Reaction deleted successfully"));
    }

    @Test
    @WithMockUser(username = "testuser") // Thiếu quyền
    void deleteReplyCommentReaction_WhenNoPermission_ShouldReturn403() throws Exception {
        mockMvc.perform(delete("/reactions/replies/30"))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------
    // 1️⃣2️⃣ GET /reactions/replies/{replyCommentId}/count/{emotionType}
    // ----------------------------------------------------------
    @Test
    @WithMockUser
    void countReplyCommentReactionsByType_WhenValid_ShouldReturn200AndCount() throws Exception {
        given(reactionService.countReplyCommentReactionsByType(30L, Reaction.EmotionType.sad)).willReturn(2L);

        // Giả sử bạn đã sửa controller dùng toLowerCase()
        mockMvc.perform(get("/reactions/replies/30/count/sad"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @WithMockUser
    void countReplyCommentReactionsByType_WhenInvalidEmotionType_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/reactions/replies/30/count/INVALID"))
                .andExpect(status().isBadRequest());
    }
}