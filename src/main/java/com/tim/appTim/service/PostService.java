// PostService.java

package com.tim.appTim.service;


import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReactionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.tim.appTim.exception.*;
import com.tim.appTim.dto.PostDTO;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.CommentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final CommentRepository commentRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository, 
                       CommentService commentService, ReactionService reactionService, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentService = commentService;
        this.reactionService = reactionService;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public PostDTO createPostWithFiles(Long userId, String content, Post.Privacy privacy, List<File> filesFromController) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        post.setPrivacy(privacy);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setTotalComments(0);
        post.setTotalReactions(0);

        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                post.addFile(file);
            }
        }

        Post savedPost = postRepository.save(post);

        // Convert files to FileDTO
        List<com.tim.appTim.dto.FileDTO> fileDTOs = savedPost.getFiles().stream()
                .map(file -> new com.tim.appTim.dto.FileDTO(
                        file.getId(),
                        file.getFileUrl(),
                        file.getFileType().name(),
                        file.getFileName() != null ? file.getFileName() : extractFileName(file.getFileUrl()),
                        file.getFileSize() != null ? file.getFileSize() : 0L
                ))
                .collect(Collectors.toList());


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
                fileDTOs,
                user.getProfileImage(),
                user.getUsername()
        );
    }
    public Page<PostDTO> getAllPosts(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("createdAt").descending()
            );
        }
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.map(this::convertToDto);
    }

    public List<PostDTO> getPostsByUserId(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return posts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PostDTO updatePostWithFiles(Long userId, Long postId, String content, Post.Privacy privacy, List<com.tim.appTim.entity.File> newFiles, List<Integer> fileIdsToDelete) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("User does not have permission to update this post");
        }

        post.setContent(content);
        post.setPrivacy(privacy);
        post.setUpdatedAt(LocalDateTime.now());

        if (fileIdsToDelete != null && !fileIdsToDelete.isEmpty()) {
            List<File> currentFiles = post.getFiles();
            currentFiles.removeIf(file -> fileIdsToDelete.contains(file.getId()));
        }

        if (newFiles != null && !newFiles.isEmpty()) {
            for (File f : newFiles) {
                post.addFile(f);
            }
        }

        Post updatedPost = postRepository.save(post);
        return convertToDto(updatedPost);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("User does not have permission to delete this post");
        }

        postRepository.delete(post);
    }

    public boolean isOwner(String username, Long postId) {
        // 1. Tìm bài post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        // 2. Tìm user đang đăng nhập bằng username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // 3. So sánh ID của user sở hữu bài post và ID của user đang đăng nhập
        return post.getUser().getId().equals(user.getId());
    }

    private PostDTO convertToDto(Post post) {
        long totalComments = commentService.countCommentsByPostId(post.getId());
        long totalReactions = reactionService.getReactionsByPostId(post.getId()).size();

        List<CommentDTO> comments = commentService.getCommentsByPostId(post.getId());
        List<ReactionDTO> reactions = reactionService.getReactionsByPostId(post.getId());

        List<com.tim.appTim.dto.FileDTO> fileDTOs = post.getFiles().stream()
                .map(file -> new com.tim.appTim.dto.FileDTO(
                        file.getId(),
                        file.getFileUrl(),
                        file.getFileType().name(),
                        file.getFileName() != null ? file.getFileName() : extractFileName(file.getFileUrl()),
                        file.getFileSize() != null ? file.getFileSize() : 0L
                ))
                .collect(Collectors.toList());

        return new PostDTO(
                post.getId(),
                post.getUser().getId(),
                post.getContent(),
                post.getPrivacy().name(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                (int) totalReactions,
                (int) totalComments,
                comments,
                reactions,
                fileDTOs,
                post.getUser().getProfileImage(),
                post.getUser().getUsername()
        );
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }


    public PostDTO getPostByIdForUser(Long userId, Long postId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("User does not have permission to access this post");
        }
        return convertToDto(post);
    }
}