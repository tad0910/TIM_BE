// PostService.java

package com.tim.appTim.service;


import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReactionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.tim.appTim.exception.*;
import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentService commentService;
    private final ReactionService reactionService;

    // Không cần FileRepository ở đây nữa nếu dùng cascade
    public PostService(PostRepository postRepository, UserRepository userRepository, 
                       CommentService commentService, ReactionService reactionService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentService = commentService;
        this.reactionService = reactionService;
    }

    @Transactional
    public PostDTO createPostWithFiles(Long userId, String content, Post.Privacy privacy, List<File> filesFromController) {
        // 1. Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 2. Tạo đối tượng Post
        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        post.setPrivacy(privacy);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setTotalComments(0);
        post.setTotalReactions(0);

        // 3. Gán các file vào Post (sử dụng phương thức tiện ích đã tạo)
        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                post.addFile(file); // <-- SỬA Ở ĐÂY: Gán Post cho File và thêm File vào List
            }
        }

        // 4. Lưu Post. Nhờ CascadeType.ALL, các File cũng sẽ được tự động lưu.
        Post savedPost = postRepository.save(post);

        // 5. Tạo DTO để trả về
        // Lưu ý: nên có FileDTO để tránh lộ chi tiết của Entity
        return new PostDTO(
                savedPost.getId(),
                savedPost.getUser().getId(),
                savedPost.getContent(),
                savedPost.getPrivacy().name(),
                savedPost.getCreatedAt(),
                savedPost.getUpdatedAt(),
                savedPost.getTotalReactions(),
                savedPost.getTotalComments(),
                new ArrayList<>(),
                new ArrayList<>(),
                savedPost.getFiles()
        );
    }
    public Page<PostDTO> getAllPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.map(this::convertToDto); // Sử dụng hàm chuyển đổi chung
    }

    public List<PostDTO> getPostsByUserId(Long userId) {
        // Kiểm tra xem user có tồn tại không
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId); // Sắp xếp theo tgian mới nhất
        return posts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PostDTO updatePost(Long userId, Long postId, String content, Post.Privacy privacy) {
        // 1. Tìm bài viết theo ID
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        // 2. ⭐ KIỂM TRA QUYỀN: Chỉ chủ nhân bài viết mới được sửa
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("User does not have permission to update this post");
        }

        // 3. Cập nhật thông tin
        post.setContent(content);
        post.setPrivacy(privacy);
        post.setUpdatedAt(LocalDateTime.now());

        // 4. Lưu lại và trả về DTO
        Post updatedPost = postRepository.save(post);
        return convertToDto(updatedPost);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        // 1. Tìm bài viết theo ID
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        // 2. ⭐ KIỂM TRA QUYỀN: Chỉ chủ nhân bài viết mới được xóa
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("User does not have permission to delete this post");
        }

        // 3. Xóa bài viết
        postRepository.delete(post);
    }

    private PostDTO convertToDto(Post post) {
        // Lấy comments và reactions cho post này
        List<CommentDTO> comments = commentService.getCommentsByPostId(post.getId());
        List<ReactionDTO> reactions = reactionService.getReactionsByPostId(post.getId());

        return new PostDTO(
                post.getId(),
                post.getUser().getId(),
                post.getContent(),
                post.getPrivacy().name(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                reactions != null ? reactions.size() : 0,
                comments != null ? comments.size() : 0,
                comments,
                reactions,
                post.getFiles()
        );
    }
}