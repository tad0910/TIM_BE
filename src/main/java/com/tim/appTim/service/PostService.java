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

    public PostService(PostRepository postRepository, UserRepository userRepository, 
                       CommentService commentService, ReactionService reactionService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentService = commentService;
        this.reactionService = reactionService;
    }

    @Transactional
    public PostDTO createPostWithFiles(Long userId, String content, Post.Privacy privacy, List<File> filesFromController) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

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

    private PostDTO convertToDto(Post post) {
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
    public PostDTO getPostByIdForUser(Long userId, Long postId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("User does not have permission to access this post");
        }
        return convertToDto(post);
    }
}