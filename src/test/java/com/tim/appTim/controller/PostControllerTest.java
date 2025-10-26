//package com.tim.appTim.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.tim.appTim.config.TestSecurityConfig;
//import com.tim.appTim.dto.PostDTO;
//import com.tim.appTim.entity.File; // Import entity File nếu cần mock service trả về
//import com.tim.appTim.entity.Post;
//import com.tim.appTim.entity.User;
//import com.tim.appTim.service.PostService;
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
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
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
//@WebMvcTest(controllers = PostController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
//@Import(TestSecurityConfig.class)
//class PostControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private PostService postService;
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
//    private PostDTO postDTO1;
//    private PostDTO postDTO2;
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
//        // --- Setup PostDTO ---
//        // Giả sử PostDTO có constructor hoặc setters phù hợp
//        postDTO1 = new PostDTO(/* Dữ liệu mẫu */);
//        postDTO1.setId(10L);
//        postDTO1.setUserId(1L);
//        postDTO1.setContent("Nội dung bài viết 1");
//        postDTO1.setPrivacy(Post.Privacy.PUBLIC.name()); // Dùng name() nếu là String
//        postDTO1.setCreatedAt(LocalDateTime.now());
//
//        postDTO2 = new PostDTO(/* Dữ liệu mẫu */);
//        postDTO2.setId(11L);
//        postDTO2.setUserId(1L);
//        postDTO2.setContent("Nội dung bài viết 2");
//        postDTO2.setPrivacy(Post.Privacy.FRIENDS.name());
//        postDTO2.setCreatedAt(LocalDateTime.now());
//    }
//
//    // ==========================================================
//    // == Tests cho Post API (/posts) ==
//    // ==========================================================
//
//    // ----------------------------------------------------------
//    // 1️⃣ POST /posts/create
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"post:create"})
//    void createPost_WhenValid_NoFiles_ShouldReturn200AndPost() throws Exception {
//        given(postService.createPostWithFiles(eq(mockUser.getId()), eq("Nội dung mới"), eq(Post.Privacy.PUBLIC), anyList()))
//                .willReturn(postDTO1); // Giả sử service trả về postDTO1
//
//        mockMvc.perform(multipart("/posts/create") // Dùng multipart vì có @RequestParam
//                        .param("content", "Nội dung mới")
//                        .param("privacy", "PUBLIC"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(10L))
//                .andExpect(jsonPath("$.content").value("Nội dung bài viết 1")); // Kiểm tra content trả về
//    }
//
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"post:create"})
//    void createPost_WhenValid_WithFiles_ShouldReturn200AndPost() throws Exception {
//        MockMultipartFile file1 = new MockMultipartFile("files", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-img".getBytes());
//        MockMultipartFile file2 = new MockMultipartFile("files", "doc.pdf", MediaType.APPLICATION_PDF_VALUE, "fake-pdf".getBytes());
//
//        given(postService.createPostWithFiles(eq(mockUser.getId()), eq("Nội dung có file"), eq(Post.Privacy.FRIENDS), anyList()))
//                .willReturn(postDTO2); // Giả sử service trả về postDTO2
//
//        mockMvc.perform(multipart("/posts/create")
//                        .file(file1)
//                        .file(file2)
//                        .param("content", "Nội dung có file")
//                        .param("privacy", "FRIENDS"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(11L))
//                .andExpect(jsonPath("$.content").value("Nội dung bài viết 2"));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"post:create"})
//    void createPost_WhenInvalidPrivacy_ShouldReturn400() throws Exception {
//        // Không cần mock service vì lỗi xảy ra trước khi gọi service
//        mockMvc.perform(multipart("/posts/create")
//                        .param("content", "Nội dung")
//                        .param("privacy", "INVALID_PRIVACY")) // Privacy không hợp lệ
//                .andExpect(status().isBadRequest()); // Mong đợi 400
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền post:create
//    void createPost_WhenNoPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(multipart("/posts/create")
//                        .param("content", "Nội dung")
//                        .param("privacy", "PUBLIC"))
//                .andExpect(status().isForbidden()); // Mong đợi 403
//    }
//
//    // ----------------------------------------------------------
//    // 2️⃣ GET /posts (Phân trang)
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser // Endpoint công khai
//    void getAllPosts_ShouldReturn200AndPage() throws Exception {
//        Pageable pageable = PageRequest.of(0, 10);
//        List<PostDTO> postList = Arrays.asList(postDTO1, postDTO2);
//        Page<PostDTO> postPage = new PageImpl<>(postList, pageable, postList.size());
//
//        given(postService.getAllPosts(any(Pageable.class))).willReturn(postPage);
//
//        mockMvc.perform(get("/posts")
//                        .param("page", "0")
//                        .param("size", "10"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.length()").value(2))
//                .andExpect(jsonPath("$.content[0].id").value(10L))
//                .andExpect(jsonPath("$.totalPages").value(1))
//                .andExpect(jsonPath("$.totalElements").value(2));
//    }
//
//    // ----------------------------------------------------------
//    // 3️⃣ GET /posts/user/{userId}
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser // Endpoint công khai
//    void getPostsByUserId_WhenPostsExist_ShouldReturn200AndList() throws Exception {
//        List<PostDTO> userPosts = Arrays.asList(postDTO1);
//        given(postService.getPostsByUserId(1L)).willReturn(userPosts);
//
//        mockMvc.perform(get("/posts/user/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(1))
//                .andExpect(jsonPath("$[0].id").value(10L));
//    }
//
//    @Test
//    @WithMockUser
//    void getPostsByUserId_WhenNoPosts_ShouldReturn200AndEmptyList() throws Exception {
//        given(postService.getPostsByUserId(2L)).willReturn(Collections.emptyList());
//
//        mockMvc.perform(get("/posts/user/2"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(0));
//    }
//
//    // ----------------------------------------------------------
//    // 4️⃣ GET /posts/{postId}/user/{userId}
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser // Endpoint công khai
//    void getPostByIdForUser_WhenPostExists_ShouldReturn200AndPost() throws Exception {
//        given(postService.getPostByIdForUser(1L, 10L)).willReturn(postDTO1);
//
//        mockMvc.perform(get("/posts/10/user/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(10L));
//    }
//
//    @Test
//    @WithMockUser
//    void getPostByIdForUser_WhenPostNotFound_ShouldReturnError() throws Exception {
//        // Giả lập service ném lỗi khi không tìm thấy bài viết
//        given(postService.getPostByIdForUser(1L, 99L)).willThrow(new RuntimeException("Post not found"));
//
//        mockMvc.perform(get("/posts/99/user/1"))
//                .andExpect(status().isInternalServerError()); // Mong đợi 500 (hoặc 404 tùy RestExceptionHandler)
//    }
//
//    // ----------------------------------------------------------
//    // 5️⃣ PUT /posts/{postId} (Chỉ test trường hợp admin)
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"post:update_all"})
//    void updatePost_WhenAdminAndValid_ShouldReturn200AndUpdatedPost() throws Exception {
//        MockMultipartFile newFile = new MockMultipartFile("files", "new.txt", MediaType.TEXT_PLAIN_VALUE, "new content".getBytes());
//        List<Integer> idsToDelete = Arrays.asList(1); // ID file cần xóa
//
//        // Cần tạo một DTO mới cho kết quả trả về nếu nội dung thay đổi
//        PostDTO updatedPostResult = new PostDTO();
//        updatedPostResult.setId(10L);
//        updatedPostResult.setContent("Nội dung đã cập nhật");
//        updatedPostResult.setPrivacy(Post.Privacy.PRIVATE.name());
//
//        given(postService.updatePostWithFiles(eq(mockUser.getId()), eq(10L), eq("Nội dung đã cập nhật"), eq(Post.Privacy.PRIVATE), anyList(), eq(idsToDelete)))
//                .willReturn(updatedPostResult);
//
//        // Sử dụng MultiValueMap để gửi cả param thường và file IDs
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("content", "Nội dung đã cập nhật");
//        params.add("privacy", "PRIVATE");
//        params.add("fileIdsToDelete", "1"); // Gửi dưới dạng String, Spring sẽ chuyển đổi
//
//        mockMvc.perform(multipart("/posts/10")
//                        .file(newFile)
//                        .params(params)
//                        .with(request -> { // Cần set method là PUT cho multipart
//                            request.setMethod("PUT");
//                            return request;
//                        }))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(10L))
//                .andExpect(jsonPath("$.content").value("Nội dung đã cập nhật"));
//    }
//
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền post:update_all
//    void updatePost_WhenNoAdminPermission_ShouldReturn403() throws Exception {
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("content", "Nội dung đã cập nhật");
//        params.add("privacy", "PRIVATE");
//
//        mockMvc.perform(multipart("/posts/10")
//                        .params(params)
//                        .with(request -> {
//                            request.setMethod("PUT");
//                            return request;
//                        }))
//                .andExpect(status().isForbidden());
//    }
//
//    // ----------------------------------------------------------
//    // 6️⃣ DELETE /posts/{postId} (Chỉ test trường hợp admin)
//    // ----------------------------------------------------------
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"post:delete_all"})
//    void deletePost_WhenAdmin_ShouldReturn200() throws Exception {
//        doNothing().when(postService).deletePost(mockUser.getId(), 10L);
//
//        mockMvc.perform(delete("/posts/10"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("Post with id 10 deleted successfully."));
//    }
//
//    @Test
//    @WithMockUser(username = "testuser") // Thiếu quyền post:delete_all
//    void deletePost_WhenNoAdminPermission_ShouldReturn403() throws Exception {
//        mockMvc.perform(delete("/posts/10"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser(username = "testuser", authorities = {"post:delete_all"})
//    void deletePost_WhenServiceThrowsError_ShouldReturnError() throws Exception {
//        doThrow(new RuntimeException("Cannot delete post")).when(postService).deletePost(mockUser.getId(), 99L);
//
//        mockMvc.perform(delete("/posts/99"))
//                .andExpect(status().isInternalServerError()); // Mong đợi 500 (hoặc 404 tùy RestExceptionHandler)
//    }
//}