package com.tim.appTim.service;

import com.tim.appTim.repository.FileRepository;
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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

import com.tim.appTim.repository.CommentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.context.annotation.Lazy;
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
    private final FileRepository fileRepository;
    private final GamificationService gamificationService;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(https?://[\\w.-]+(?:\\:[0-9]+)?(?:/[^\\s]*)?)\\b",
            Pattern.CASE_INSENSITIVE);

    public PostService(PostRepository postRepository, UserRepository userRepository,
            CommentService commentService, ReactionService reactionService, CommentRepository commentRepository,
            LinkPreviewService linkPreviewService, FileRepository fileRepository,
            @Lazy GamificationService gamificationService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentService = commentService;
        this.reactionService = reactionService;
        this.commentRepository = commentRepository;
        this.linkPreviewService = linkPreviewService;
        this.fileRepository = fileRepository;
        this.gamificationService = gamificationService;
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
        return files.stream().anyMatch(f -> f.getFileType() == File.FileType.IMAGE ||
                f.getFileType() == File.FileType.VIDEO);
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
    public PostDTO createPostWithFiles(Long userId, String content, Post.Privacy privacy,
            List<File> filesFromController) {

        boolean isFilesEmpty = (filesFromController == null || filesFromController.isEmpty());

        if ((content == null || content.trim().isEmpty()) && isFilesEmpty) {
            throw new UnprocessableException("Bài viết phải có nội dung hoặc tệp đính kèm.");
        }
        if (content == null) {
            content = "";
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
        try {
            long postCount = postRepository.countByUserId(userId);
            if (postCount == 1) {
                gamificationService.awardPoints(userId, "FIRST_POST");
            }
        } catch (Exception e) {
            System.err.println("Failed to award points for first post: " + e.getMessage());
        }

        if (savedPost.getLinkUrl() != null) {
            try {
                gamificationService.awardPoints(userId, "POST_SHARE");
            } catch (Exception e) {
                System.err.println("Failed to award points for post share: " + e.getMessage());
            }
        }

        List<com.tim.appTim.dto.FileDTO> fileDTOs = savedPost.getFiles().stream()
                .map(file -> new com.tim.appTim.dto.FileDTO(
                        file.getId(),
                        file.getFileUrl(),
                        file.getFileType().name(),
                        file.getFileName() != null ? file.getFileName() : extractFileName(file.getFileUrl()),
                        file.getFileSize() != null ? file.getFileSize() : 0L))
                .collect(Collectors.toList());

        String displayName = getUserDisplayName(user);

        LinkPreviewDTO linkPreview = null;
        if (savedPost.getLinkUrl() != null && savedPost.hasLinkPreview()) {
            linkPreview = new LinkPreviewDTO(
                    savedPost.getLinkUrl(),
                    savedPost.getLinkTitle(),
                    savedPost.getLinkDescription(),
                    savedPost.getLinkImageUrl(),
                    savedPost.getLinkDomain());
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
                linkPreview);
    }

    @Transactional(readOnly = true)
    public Page<PostDTO> getAllPosts(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("createdAt").descending());
        }
        Page<Post> postPage = postRepository.findAll(pageable);
        return postPage.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<PostDTO> getPostsByUserId(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("createdAt").descending());
        }

        Page<Post> postsPage = postRepository.findByUserId(userId, pageable);

        return postsPage.map(this::convertToDto);
    }

    @Transactional
    public PostDTO updatePostWithFiles(User currentUser, Authentication authentication, Long postId, String content,
            Post.Privacy privacy,
            List<com.tim.appTim.entity.File> newFiles, List<Integer> fileIdsToDelete) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("post:update_all"));
        boolean isOwner = post.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("User does not have permission to update this post");
        }

        if (content != null) {
            post.setContent(content);
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
                fileRepository.save(f);
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
                .anyMatch(a -> a.getAuthority().equals("post:update_all"));
        boolean isOwner = post.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("User does not have permission to update this post");
        }

        postRepository.delete(post);
    }

    public boolean isOwner(String username, Long postId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        return postRepository.findById(postId)
                .map(post -> post.getUser().getId().equals(user.getId()))
                .orElse(false);
    }

    private PostDTO convertToDto(Post post) {

        int totalReactions = post.getTotalReactions() != null ? post.getTotalReactions() : 0;
        int totalComments = post.getTotalComments() != null ? post.getTotalComments() : 0;

        List<com.tim.appTim.dto.FileDTO> fileDTOs = post.getFiles().stream()
                .map(file -> new com.tim.appTim.dto.FileDTO(
                        file.getId(),
                        file.getFileUrl(),
                        file.getFileType().name(),
                        file.getFileName() != null ? file.getFileName() : extractFileName(file.getFileUrl()),
                        file.getFileSize() != null ? file.getFileSize() : 0L))
                .collect(Collectors.toList());

        String displayName = getUserDisplayName(post.getUser());

        LinkPreviewDTO linkPreview = null;
        if (post.getLinkUrl() != null && post.hasLinkPreview()) {
            linkPreview = new LinkPreviewDTO(
                    post.getLinkUrl(),
                    post.getLinkTitle(),
                    post.getLinkDescription(),
                    post.getLinkImageUrl(),
                    post.getLinkDomain());
        }

        return new PostDTO(
                post.getId(),
                post.getUser().getId(),
                post.getContent(),
                post.getPrivacy().name(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                totalReactions,
                totalComments,
                new ArrayList<>(),
                new ArrayList<>(),
                fileDTOs,
                post.getUser().getProfileImage(),
                post.getUser().getUsername(),
                displayName,
                linkPreview);
    }

    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        String[] parts = fileUrl.split("/");
        return parts[parts.length - 1];
    }

    public PostDTO getPostByIdForUser(Long requestingUserId, Long postId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        Long postOwnerId = post.getUser().getId();

        if (requestingUserId.equals(postOwnerId)) {
            return convertToDto(post);
        }
        Post.Privacy privacy = post.getPrivacy();

        switch (privacy) {
            case only_me:
                throw new ForbiddenException("You do not have permission to access this post");
            case friends:
                return convertToDto(post);
            case open:
                return convertToDto(post);
            default:
                throw new ForbiddenException("You do not have permission to access this post");
        }
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean isPostOwner(Authentication authentication, Long postId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        final String currentUsername = extractUsername(authentication);
        if (currentUsername == null) {
            return false;
        }

        return postRepository.findById(postId)
                .map(post -> {
                    User owner = post.getUser();
                    return owner != null && currentUsername.equals(owner.getUsername());
                })
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String username) {
            return username;
        }

        if (principal instanceof String token && token.startsWith("Bearer ")) {
            token.substring(7);
        } else {
            return null;
        }

        try {
            JwtParser parser = Jwts.parserBuilder()
                    .build();

            Claims claims = parser.parseClaimsJws((String) principal).getBody();

            String preferredUsername = claims.get("preferred_username", String.class);
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            return claims.getSubject();

        } catch (Exception e) {
            System.err.println("Failed to parse JWT: " + e.getMessage());
            return null;
        }
    }
}