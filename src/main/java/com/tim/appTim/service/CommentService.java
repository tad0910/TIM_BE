package com.tim.appTim.service;

import com.tim.appTim.dto.CommentDTO;
import com.tim.appTim.dto.ReplyCommentDTO;
import com.tim.appTim.entity.Comment;
import com.tim.appTim.entity.Post;
import com.tim.appTim.entity.ReplyComment;
import com.tim.appTim.entity.File;
import com.tim.appTim.entity.User;
import com.tim.appTim.exception.ResourceNotFoundException;
import com.tim.appTim.exception.ForbiddenException;
import com.tim.appTim.exception.BadRequestException;
import com.tim.appTim.repository.CommentRepository;
import com.tim.appTim.repository.PostRepository;
import com.tim.appTim.repository.ReplyCommentRepository;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.ReactionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import com.tim.appTim.repository.FileRepository;
import com.tim.appTim.dto.FileDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final ReplyCommentRepository replyCommentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ReactionRepository reactionRepository;
    private final FileRepository fileRepository;
    private final NotificationService notificationService;

    @Autowired
    public CommentService(CommentRepository commentRepository,
                          ReplyCommentRepository replyCommentRepository,
                          @Lazy UserService userService,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          @Lazy NotificationService notificationService,
                          ReactionRepository reactionRepository,
                          FileRepository fileRepository) {
        this.commentRepository = commentRepository;
        this.replyCommentRepository = replyCommentRepository;
        this.userService = userService;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.reactionRepository = reactionRepository;
        this.fileRepository = fileRepository;
    }



    @Transactional
    public CommentDTO createComment(Long postId, Long userId, String content,
                                    Comment.Emotion emotion, List<File> filesFromController) { 

        if ((content == null || content.trim().isEmpty()) && (filesFromController == null || filesFromController.isEmpty())) {
            throw new BadRequestException("Bạn phải cung cấp nội dung hoặc tệp đính kèm.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setEmotion(emotion);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        List<FileDTO> fileDTOs = new ArrayList<>();

        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                comment.addFile(file);
            }
        }

        Comment savedComment = commentRepository.save(comment);

        Integer currentTotal = post.getTotalComments();
        post.setTotalComments(currentTotal == null ? 1 : currentTotal + 1);
        postRepository.save(post);

        try {
            if (!post.getUser().getId().equals(userId)) {
                notificationService.createCommentNotification(
                        post.getUser().getId(), null, userId, user.getUsername(), "POST", postId
                );
            }
        } catch (Exception e) { System.err.println("Error creating notification: " + e.getMessage()); }

        return convertToDTO(savedComment);
    }

    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsByPostId(Long postId, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByPostId(postId, pageable);
        return commentPage.map(this::convertToDTO);
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
    }

    public ReplyComment getReplyCommentById(Long replyCommentId) {
        return replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));
    }

    @Transactional
    public CommentDTO updateComment(Long commentId, User currentUser, Authentication authentication, String content, Comment.Emotion emotion, List<File> newFilesFromController) {

        if ((content == null || content.trim().isEmpty()) && (newFilesFromController == null || newFilesFromController.isEmpty())) {
            throw new BadRequestException("Bạn phải cung cấp nội dung hoặc tệp đính kèm.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:update_all"));
        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());
        boolean isPostOwner = comment.getPost().getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner && !isPostOwner) {
            throw new ForbiddenException("Bạn không có quyền sửa bình luận này");
        }

        comment.setContent(content);
        comment.setEmotion(emotion);

        if (comment.getFiles() != null) {
            fileRepository.deleteAll(comment.getFiles());
            comment.getFiles().clear();
        }

        if (newFilesFromController != null && !newFilesFromController.isEmpty()) {
            for (File file : newFilesFromController) {
                comment.addFile(file);
            }
        }

        comment.setUpdatedAt(LocalDateTime.now());

        Comment updatedComment = commentRepository.save(comment);
        return convertToDTO(updatedComment);
    }

    @Transactional
    public void deleteComment(Long commentId, User currentUser, Authentication authentication) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:delete_all"));
        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());
        boolean isPostOwner = comment.getPost().getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner && !isPostOwner) {
            throw new ForbiddenException("Bạn không có quyền xóa bình luận này");
        }

        Post post = comment.getPost();
        if (post != null) {
            Integer currentTotal = post.getTotalComments();
            post.setTotalComments(currentTotal == null || currentTotal <= 0 ? 0 : currentTotal - 1);
            postRepository.save(post);
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public ReplyCommentDTO createReplyComment(Long commentId, Long userId, String content,
                                              ReplyComment.Emotion emotion, List<File> filesFromController) {

        if ((content == null || content.trim().isEmpty()) && (filesFromController == null || filesFromController.isEmpty())) {
            throw new BadRequestException("Bạn phải cung cấp nội dung hoặc tệp đính kèm.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        ReplyComment reply = new ReplyComment();
        reply.setComment(comment);
        reply.setUser(user);
        reply.setContent(content);
        reply.setEmotion(emotion);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());

        if (filesFromController != null && !filesFromController.isEmpty()) {
            for (File file : filesFromController) {
                reply.addFile(file);
            }
        }

        ReplyComment savedReplyComment = replyCommentRepository.save(reply);

        try {
            if (!comment.getUser().getId().equals(userId)) {
                notificationService.createCommentNotification(
                        null, comment.getUser().getId(), userId, user.getUsername(), "COMMENT", commentId
                );
            }
        } catch (Exception e) { System.err.println("Error creating notification: " + e.getMessage()); }

        return convertReplyToDTO(savedReplyComment);
}
    @Transactional(readOnly = true)
    public Page<ReplyCommentDTO> getReplyCommentsByCommentId(Long commentId, Pageable pageable) {
        Page<ReplyComment> replyPage = replyCommentRepository.findByCommentId(commentId, pageable);
        return replyPage.map(this::convertReplyToDTO);
    }

    public ReplyCommentDTO updateReplyComment(User currentUser, Authentication authentication, Long replyCommentId, String content, ReplyComment.Emotion emotion, List<File> newFilesFromController) {

        if ((content == null || content.trim().isEmpty()) && (newFilesFromController == null || newFilesFromController.isEmpty())) {
            throw new BadRequestException("Bạn phải cung cấp nội dung hoặc tệp đính kèm.");
        }

        ReplyComment reply = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:update_all"));
        boolean hasReplyPermission = reply.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !hasReplyPermission) {
            throw new ForbiddenException("Bạn không có quyền sửa trả lời này");
        }

        reply.setContent(content);
        reply.setEmotion(emotion);

        if (reply.getFiles() != null) {
            fileRepository.deleteAll(reply.getFiles());
            reply.getFiles().clear();
        }

        if (newFilesFromController != null && !newFilesFromController.isEmpty()) {
            for (File file : newFilesFromController) {
                reply.addFile(file);
            }
        }
        reply.setUpdatedAt(LocalDateTime.now());

        ReplyComment updatedReply = replyCommentRepository.save(reply);
        return convertReplyToDTO(updatedReply);
    }

    public void deleteReplyComment(User currentUser, Authentication authentication, Long replyCommentId) {
        ReplyComment replyComment = replyCommentRepository.findById(replyCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyCommentId));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("comment:delete_all"));
        boolean hasReplyPermission = replyComment.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !hasReplyPermission) {
            throw new ForbiddenException("Bạn không có quyền xóa trả lời này");
        }

        replyCommentRepository.delete(replyComment);
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userService.findByUsernameOrEmail(authentication.getName());
    }

    private CommentDTO convertToDTO(Comment comment) {
        User user = comment.getUser();
        String username = (user != null) ? user.getUsername() : "Unknown";
        String userAvatar = (user != null) ? user.getProfileImage() : " ";
        String emotionName = (comment.getEmotion() != null) ? comment.getEmotion().name() : null;

        List<ReplyCommentDTO> replies = comment.getReplies()
                .stream()
                .map(this::convertReplyToDTO)
                .collect(Collectors.toList());

       List<FileDTO> fileDTOs = new ArrayList<>();
        if (comment.getFiles() != null && !comment.getFiles().isEmpty()) {
fileDTOs = comment.getFiles().stream()
                    .map(file -> new FileDTO(
                            file.getId(),
                            file.getFileUrl(),
                            file.getFileType().name(),
                            file.getFileName(),
                            file.getFileSize()
                    ))
                    .collect(Collectors.toList());
        }

        return new CommentDTO(
                comment.getId(),
                comment.getUser().getId(),
                username,
                comment.getContent(),
                userAvatar,
                emotionName,
                comment.getCreatedAt(),
                replies,
                fileDTOs
        );
    }

    private ReplyCommentDTO convertReplyToDTO(ReplyComment reply) {
        String username = reply.getUser() != null ? reply.getUser().getUsername() : "Unknown";
        String emotionName = reply.getEmotion() != null ? reply.getEmotion().name() : null;
        return new ReplyCommentDTO(
                reply.getId(),
                reply.getComment().getId(),
                reply.getUser().getId(),
                username,
                reply.getContent(),
                emotionName,
//                reply.getFileId(),
                reply.getCreatedAt(),
                reply.getUser() != null ? reply.getUser().getProfileImage() : " ",
                reply.getFiles() != null ? reply.getFiles().stream()
                        .map(file -> new FileDTO(
                                file.getId(),
                                file.getFileUrl(),
                                file.getFileType().name(),
                                file.getFileName(),
                                file.getFileSize()
                        ))
                        .collect(Collectors.toList()) : new ArrayList<>()
        );
    }

    public boolean isOwner(Authentication authentication, Long commentId) {
        User currentUser = getUserFromAuthentication(authentication);
        return commentRepository.findById(commentId)
                .map(comment -> comment.getUser().getId().equals(currentUser.getId()))
                .orElse(false);
    }   

    public boolean isReplyOwner(Authentication authentication, Long replyCommentId) {
        User currentUser = getUserFromAuthentication(authentication);
        return replyCommentRepository.findById(replyCommentId)
.map(reply -> reply.getUser().getId().equals(currentUser.getId()))
                .orElse(false);
    }

    public long countCommentsByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean hasCommentPermission(Authentication authentication, Long commentId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        final String currentUsername = extractUsername(authentication);
        if (currentUsername == null) return false;

        return commentRepository.findById(commentId)
                .map(comment -> {
                    User commentOwner = comment.getUser();
                    User postOwner = comment.getPost().getUser();

                    boolean isCommentOwner = commentOwner != null && currentUsername.equals(commentOwner.getUsername());
                    boolean isPostOwner = postOwner != null && currentUsername.equals(postOwner.getUsername());
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("comment:update_all") || a.getAuthority().equals("comment:delete_all"));

                    return isCommentOwner || isPostOwner || isAdmin;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId)); 
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean hasReplyPermission(Authentication authentication, Long replyId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        final String currentUsername = extractUsername(authentication);
        if (currentUsername == null) return false;

        return replyCommentRepository.findById(replyId)
                .map(reply -> {
                    User replyOwner = reply.getUser();
                    User postOwner = reply.getComment().getPost().getUser();

                    boolean isReplyOwner = replyOwner != null && currentUsername.equals(replyOwner.getUsername());
                    boolean isPostOwner = postOwner != null && currentUsername.equals(postOwner.getUsername());
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("comment:update_all") || a.getAuthority().equals("comment:delete_all"));

                    return isReplyOwner || isPostOwner || isAdmin;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Reply comment not found with id: " + replyId));
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof String username) {
            return username;
        }

        if (principal instanceof String token) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"(sub|preferred_username)\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
                    if (m.find()) {
                        return m.group(2);
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to extract username from JWT: " + e.getMessage());
            }
        }

        return null;
    }
}