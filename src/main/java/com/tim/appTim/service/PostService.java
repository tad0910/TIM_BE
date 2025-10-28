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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.tim.appTim.dto.LinkPreviewDTO;
import org.springframework.scheduling.annotation.Async;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URISyntaxException;
import java.io.IOException;







@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final CommentRepository commentRepository;
    private final LinkPreviewService linkPreviewService;

    private static final Pattern URL_PATTERN = Pattern.compile(
        "\\b(https?://[\\w.-]+(?:\\:[0-9]+)?(?:/[^\\s]*)?)\\b",
        Pattern.CASE_INSENSITIVE
    );

    public PostService(PostRepository postRepository, UserRepository userRepository, 
                       CommentService commentService, ReactionService reactionService, CommentRepository commentRepository,
                       LinkPreviewService linkPreviewService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentService = commentService;
        this.reactionService = reactionService;
        this.commentRepository = commentRepository;
        this.linkPreviewService = linkPreviewService;
    }

    private String extractFirstUrl(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
    private boolean hasMediaFiles(List<File> files) {
        if (files == null || files.isEmpty()) {
            return false;
        }
        return files.stream().anyMatch(f ->
            f.getFileType() == File.FileType.IMAGE ||
            f.getFileType() == File.FileType.VIDEO
        );
    }
    @Async("linkPreviewTaskExecutor")
    public void generateLinkPreviewAsync(Long postId, String url) {
        try {
            System.out.println("[Async] Generating link preview for post: " + postId + ", URL: " + url);
            LinkPreviewDTO preview = linkPreviewService.getLinkPreview(url);
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null) {
                post.setLinkTitle(preview.title());
                post.setLinkDescription(preview.description());
                post.setLinkImageUrl(preview.imageUrl());
                post.setLinkDomain(preview.domain());
                post.setUpdatedAt(LocalDateTime.now());
                postRepository.save(post);
                System.out.println("[Async] :white_check_mark: Link preview generated for post: " + postId);
            } else {
                System.err.println("[Async] :x: Post not found: " + postId);
            }
        } catch (URISyntaxException e) {
            System.err.println("[Async] :x: Invalid URL: " + url);
        } catch (IOException e) {
            System.err.println("[Async] :x: Failed to fetch preview for: " + url);
            System.err.println("   Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Async] :x: Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public PostDTO createPostWithFiles(Long userId, String content, Post.Privacy privacy, List<File> filesFromController) {

        if (userId == null) {
            throw new UnprocessableException("User ID không được để trống");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new UnprocessableException("Nội dung bài viết không được để trống");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với id: " + userId));

        boolean hasMedia = hasMediaFiles(filesFromController);

        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        post.setPrivacy(privacy);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setTotalComments(0);
        post.setTotalReactions(0);

        if (!hasMedia) {
            String firsturl = extractFirstUrl(content);
            if (firsturl != null) {
                post.setLinkUrl(firsturl);
            }
        }

        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                post.addFile(file);
            }
        }

        
        Post savedPost = postRepository.save(post);

        if (savedPost.getLinkUrl() != null && !hasMedia) {
            generateLinkPreviewAsync(savedPost.getId(), savedPost.getLinkUrl());
        }
        
        List<com.tim.appTim.dto.FileDTO> fileDTOs = savedPost.getFiles().stream()
                .map(file -> new com.tim.appTim.dto.FileDTO(
                        file.getId(),
                        file.getFileUrl(),
                        file.getFileType().name(),
                        file.getFileName() != null ? file.getFileName() : extractFileName(file.getFileUrl()),
                        file.getFileSize() != null ? file.getFileSize() : 0L
                ))
                .collect(Collectors.toList());

        String displayName = getUserDisplayName(user);

        LinkPreviewDTO linkPreview = null;
        if (savedPost.getLinkUrl() != null && savedPost.hasLinkPreview()) {
            linkPreview = new LinkPreviewDTO(
                savedPost.getLinkUrl(),
                savedPost.getLinkTitle(),
                savedPost.getLinkDescription(),
                savedPost.getLinkImageUrl(),
                savedPost.getLinkDomain()
            );
        }

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
                user.getUsername(),
                displayName,
                linkPreview
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
    public PostDTO updatePostWithFiles(User currentUser, Authentication authentication, Long postId, String content, Post.Privacy privacy,
                                       List<com.tim.appTim.entity.File> newFiles, List<Integer> fileIdsToDelete) {
        if (content == null || content.trim().isEmpty()) {
            throw new UnprocessableException("Content cannot be empty");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("post:update_all"));

        // Lấy quyền chủ sở hữu
        boolean isOwner = post.getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("User does not have permission to update this post");
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

        boolean hasMedia = hasMediaFiles(post.getFiles());
        String newUrl = extractFirstUrl(content);
        String oldUrl = post.getLinkUrl();
        if (hasMedia) {
            post.setLinkUrl(null);
            post.setLinkTitle(null);
            post.setLinkDescription(null);
            post.setLinkImageUrl(null);
            post.setLinkDomain(null);
        } else if (newUrl != null && !newUrl.equals(oldUrl)) {
            post.setLinkUrl(newUrl);
            post.setLinkTitle(null);
            post.setLinkDescription(null);
            post.setLinkImageUrl(null);
            post.setLinkDomain(null);
            generateLinkPreviewAsync(postId, newUrl);
        } else if (newUrl == null && oldUrl != null) {
            post.setLinkUrl(null);
            post.setLinkTitle(null);
            post.setLinkDescription(null);
            post.setLinkImageUrl(null);
            post.setLinkDomain(null);
        } else if (newUrl != null && newUrl.equals(oldUrl) && !post.hasLinkPreview()) {
            generateLinkPreviewAsync(postId, newUrl);
        }

        Post updatedPost = postRepository.save(post);
        return convertToDto(updatedPost);
    }


    @Transactional
    public void deletePost(User currentUser, Authentication authentication, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("post:delete_all")); // Giả sử quyền là 'post:delete_all'
        boolean isOwner = post.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("User does not have permission to update this post");
        }

        postRepository.delete(post);
    }

    public boolean isOwner(String username, Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
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

        String displayName = getUserDisplayName(post.getUser());

        LinkPreviewDTO linkPreview = null;
        if (post.getLinkUrl() != null && post.hasLinkPreview()) {
            linkPreview = new LinkPreviewDTO(
                post.getLinkUrl(),
                post.getLinkTitle(),
                post.getLinkDescription(),
                post.getLinkImageUrl(),
                post.getLinkDomain()
            );
        }

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
                post.getUser().getUsername(),
                displayName,
                linkPreview
        );
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }

    private String getUserDisplayName(User user) {
        if (user == null) {
            return "Người dùng";
        }
        
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String username = user.getUsername();
        
        if (firstName != null && !firstName.trim().isEmpty() && 
            lastName != null && !lastName.trim().isEmpty()) {
            return firstName + " " + lastName;
        }
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName;
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName;
        }
        
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        
        return "Người dùng";
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